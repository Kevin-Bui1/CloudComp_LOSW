package com.legends.pve.service;

import com.legends.pve.dto.*;
import com.legends.pve.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PveController — the core service class for PvE campaign logic.
 *
 * Campaign flow:
 *   startCampaign → nextRoom (x30) → calculateScore → endCampaign
 *   restoreCampaign is the "continue saved game" path.
 *
 * Battle integration (Fix 5):
 *   nextRoom returns enemies + rewards when roomType == BATTLE.
 *   The client then talks to battle-service directly for turn-by-turn combat.
 *   When the battle ends, the client calls POST /{userId}/battle/resolve
 *   with the outcome so this service can apply XP and gold to the party.
 */
@Service
public class PveController {

    private final RoomFactory  roomFactory;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${battle.service.url:http://localhost:5001}")
    private String battleServiceUrl;

    /** One Campaign per logged-in user, keyed by userId. */
    private final Map<Long, Campaign> activeCampaigns = new ConcurrentHashMap<>();

    /**
     * Holds the pending battle rewards for the current room so resolveBattle
     * can apply them without recalculating after the fight.
     * Keyed by userId → { "expReward": int, "goldReward": int }
     */
    private final Map<Long, Map<String, Integer>> pendingRewards = new ConcurrentHashMap<>();

    public PveController(RoomFactory roomFactory) {
        this.roomFactory = roomFactory;
    }

    // ── Campaign lifecycle ─────────────────────────────────────────────────

    public CampaignResponse startCampaign(Long userId, List<HeroRequest> heroRequests) {
        Party party = new Party();
        for (HeroRequest req : heroRequests) {
            party.addHero(new Hero(req.getName(), req.getHeroClass()));
        }
        Campaign campaign = new Campaign(party);
        activeCampaigns.put(userId, campaign);
        return CampaignResponse.of(campaign, campaign.start(), null);
    }

    /**
     * Advances to the next room. For battle rooms:
     *   - Creates a battle session on the battle-service (POST /api/battle/{id}/start)
     *   - Stores the pending rewards so resolveBattle can apply them later
     *   - Returns the battle session ID, enemies, and rewards to the client
     *
     * For inn rooms:
     *   - Heals the party immediately and returns the shop/recruit catalogue
     */
    public CampaignResponse nextRoom(Long userId) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null)   return CampaignResponse.error("No active campaign found.");
        if (campaign.isComplete()) return CampaignResponse.error("Campaign complete! Check your score.");

        campaign.advanceRoom();
        Room room = roomFactory.generateRoom(
                campaign.getCurrentRoom(),
                campaign.getParty().getCumulativeLevel()
        );

        String roomDescription = room.enter(campaign.getParty());
        String roomType        = room instanceof BattleRoom ? "BATTLE" : "INN";
        CampaignResponse response = CampaignResponse.of(campaign, roomDescription, roomType);

        if (room instanceof BattleRoom battleRoom) {
            // Store pending rewards so resolveBattle can apply them
            Map<String, Integer> rewards = new HashMap<>();
            rewards.put("expReward",  battleRoom.calculateTotalExp());
            rewards.put("goldReward", battleRoom.calculateTotalGold());
            pendingRewards.put(userId, rewards);

            // Register a battle session on the battle-service so the client
            // can send action requests directly to it.
            String battleId = "pve-" + userId + "-room-" + campaign.getCurrentRoom();
            try {
                Map<String, Object> battleRequest = buildBattleRequest(
                        campaign.getParty(), battleRoom.getEnemies(), battleId);
                restTemplate.postForObject(
                        battleServiceUrl + "/api/battle/" + battleId + "/start",
                        battleRequest, Map.class);
                response.setBattleId(battleId);
            } catch (Exception e) {
                // Battle service unreachable — surface the ID to the client anyway
                // so it can still attempt a direct connection.
                response.setBattleId(battleId);
                System.err.println("Warning: could not pre-register battle on battle-service: " + e.getMessage());
            }

            response.setEnemies(battleRoom.getEnemies());
            response.setExpReward(rewards.get("expReward"));
            response.setGoldReward(rewards.get("goldReward"));
        }

        return response;
    }

    /**
     * Called by the client after a battle finishes to apply XP and gold rewards.
     *
     * If playerWon == true:  party gains gold and all heroes gain XP.
     * If playerWon == false: party loses 20% of its gold as a death penalty.
     *
     * heroExpGained is per-hero XP (usually totalExp / number of heroes).
     */
    public CampaignResponse resolveBattle(Long userId, boolean playerWon) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return CampaignResponse.error("No active campaign.");

        Map<String, Integer> rewards = pendingRewards.remove(userId);
        if (rewards == null)  return CampaignResponse.error("No pending battle for this campaign.");

        Party party = campaign.getParty();
        StringBuilder msg = new StringBuilder();

        if (playerWon) {
            int gold = rewards.get("goldReward");
            int exp  = rewards.get("expReward");
            party.addGold(gold);

            // Distribute XP evenly across living heroes
            List<Hero> living = party.getHeroes().stream()
                    .filter(h -> h.getHp() > 0).toList();
            if (!living.isEmpty()) {
                int perHero = exp / living.size();
                for (Hero h : living) {
                    int oldLevel = h.getLevel();
                    h.gainExperience(perHero);
                    if (h.getLevel() > oldLevel) {
                        msg.append(h.getName()).append(" levelled up to ")
                           .append(h.getLevel()).append("! ");
                    }
                }
            }
            msg.insert(0, "Victory! Gained " + gold + "g and " + exp + " XP. ");
        } else {
            int penalty = party.getGold() / 5;
            party.addGold(-penalty);
            msg.append("Defeated. Lost ").append(penalty).append("g. ");
            // Revive all heroes at 1 HP so the campaign can continue
            for (Hero h : party.getHeroes()) {
                if (h.getHp() <= 0) {
                    h.setHp(1);
                    msg.append(h.getName()).append(" revived at 1 HP. ");
                }
            }
        }

        return CampaignResponse.of(campaign, msg.toString().trim(), "BATTLE");
    }

    public CampaignResponse getCampaign(Long userId) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return CampaignResponse.error("No active campaign.");
        return CampaignResponse.of(campaign, "Campaign info retrieved.", null);
    }

    public CampaignResponse restoreCampaign(Long userId, SavedStateRequest savedState) {
        Party party = new Party();
        party.setGold(savedState.getGold());
        for (HeroRequest hr : savedState.getHeroes()) {
            Hero h = new Hero(hr.getName(), hr.getHeroClass());
            h.setLevel(hr.getLevel());
            h.setAttack(hr.getAttack());
            h.setDefense(hr.getDefense());
            h.setHp(hr.getHp());
            h.setMaxHp(hr.getMaxHp());
            h.setMana(hr.getMana());
            h.setMaxMana(hr.getMaxMana());
            party.addHero(h);
        }
        Campaign campaign = new Campaign(party);
        campaign.setCurrentRoom(savedState.getCurrentRoom());
        activeCampaigns.put(userId, campaign);
        return CampaignResponse.of(campaign, "Campaign restored from save.", null);
    }

    public int calculateScore(Long userId) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return 0;
        Party party = campaign.getParty();
        int levelScore = party.getHeroes().stream().mapToInt(h -> h.getLevel() * 100).sum();
        int goldScore  = party.getGold() * 10;
        return levelScore + goldScore;
    }

    public void endCampaign(Long userId) {
        activeCampaigns.remove(userId);
        pendingRewards.remove(userId);
    }

    // ── Inn actions ────────────────────────────────────────────────────────

    public CampaignResponse buyItem(Long userId, String itemName) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return CampaignResponse.error("No active campaign.");

        Object[] found = findItem(itemName);
        if (found == null)
            return CampaignResponse.error("Item '" + itemName + "' is not sold at this inn.");

        int cost = (int) found[1];
        if (!campaign.getParty().deductGold(cost))
            return CampaignResponse.error("Not enough gold. Need " + cost + "g, have "
                    + campaign.getParty().getGold() + "g.");

        campaign.getParty().addItem(itemName, 1);
        return CampaignResponse.of(campaign,
                "Bought " + itemName + " for " + cost + "g. Gold remaining: "
                + campaign.getParty().getGold() + "g.", "INN");
    }

    public CampaignResponse recruitHero(Long userId, String heroName, String heroClass) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return CampaignResponse.error("No active campaign.");

        Party party = campaign.getParty();
        if (party.getHeroes().size() >= 5)
            return CampaignResponse.error("Party is full — max 5 heroes.");

        int cost = 300 + 50 * campaign.getCurrentRoom();
        if (!party.deductGold(cost))
            return CampaignResponse.error("Not enough gold. Need " + cost + "g, have "
                    + party.getGold() + "g.");

        party.addHero(new Hero(heroName, heroClass));
        return CampaignResponse.of(campaign,
                "Recruited " + heroName + " [" + heroClass + "] for " + cost + "g.", "INN");
    }

    public CampaignResponse useItem(Long userId, String itemName, int heroIndex) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return CampaignResponse.error("No active campaign.");

        Party party = campaign.getParty();
        if (heroIndex < 0 || heroIndex >= party.getHeroes().size())
            return CampaignResponse.error("Invalid hero index: " + heroIndex);
        if (!party.useItem(itemName))
            return CampaignResponse.error("Item '" + itemName + "' not in inventory.");

        Hero hero = party.getHeroes().get(heroIndex);
        Object[] found = findItem(itemName);
        StringBuilder effect = new StringBuilder(hero.getName() + " used " + itemName + ". ");

        if (found != null) {
            int hpBonus      = (int) found[2];
            int manaBonus    = (int) found[3];
            int attackBonus  = (int) found[4];
            int defenseBonus = (int) found[5];
            if (hpBonus > 0) {
                int healed = Math.min(hpBonus, hero.getMaxHp() - hero.getHp());
                hero.setHp(Math.min(hero.getHp() + hpBonus, hero.getMaxHp()));
                effect.append("+").append(healed).append(" HP. ");
            }
            if (manaBonus > 0) {
                hero.setMana(Math.min(hero.getMana() + manaBonus, hero.getMaxMana()));
                effect.append("+").append(manaBonus).append(" mana. ");
            }
            if (attackBonus > 0) {
                hero.setAttack(hero.getAttack() + attackBonus);
                effect.append("+").append(attackBonus).append(" attack. ");
            }
            if (defenseBonus > 0) {
                hero.setDefense(hero.getDefense() + defenseBonus);
                effect.append("+").append(defenseBonus).append(" defense. ");
            }
        }
        return CampaignResponse.of(campaign, effect.toString().trim(), "INN");
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private Object[] findItem(String itemName) {
        for (Object[] item : InnRoom.ITEMS) {
            if (item[0].equals(itemName)) return item;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildBattleRequest(Party party, List<Enemy> enemies, String battleId) {
        List<Map<String, Object>> playerUnits = party.getHeroes().stream().map(h -> {
            Map<String, Object> u = new HashMap<>();
            u.put("name",    h.getName());
            u.put("level",   h.getLevel());
            u.put("attack",  h.getAttack());
            u.put("defense", h.getDefense());
            u.put("hp",      h.getHp());
            u.put("maxHp",   h.getMaxHp());
            u.put("mana",    h.getMana());
            u.put("maxMana", h.getMaxMana());
            u.put("heroClass", h.getHeroClass());
            return u;
        }).toList();

        List<Map<String, Object>> enemyUnits = enemies.stream().map(e -> {
            Map<String, Object> u = new HashMap<>();
            u.put("name",    e.getName());
            u.put("level",   e.getLevel());
            u.put("attack",  e.getAttack());
            u.put("defense", e.getDefense());
            u.put("hp",      e.getHp());
            u.put("maxHp",   e.getMaxHp());
            u.put("mana",    0);
            u.put("maxMana", 0);
            return u;
        }).toList();

        Map<String, Object> request = new HashMap<>();
        request.put("playerUnits", playerUnits);
        request.put("enemyUnits",  enemyUnits);
        return request;
    }
}
