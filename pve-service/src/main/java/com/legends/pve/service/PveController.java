package com.legends.pve.service;

import com.legends.pve.dto.*;
import com.legends.pve.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

/**
 * PveController — stateless core service class for PvE campaign logic.
 *
 * STATELESS DESIGN:
 *   No campaign state is held in memory. Every mutating request receives the
 *   full campaign state (heroes, gold, currentRoom, inventory) from the client
 *   and returns the updated state in the response. The client tracks live
 *   session state; the data-service handles persistence. This allows
 *   pve-service to scale horizontally and restart without data loss.
 */
@Service
public class PveController {

    private final RoomFactory  roomFactory;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${battle.service.url:http://localhost:5001}")
    private String battleServiceUrl;

    @Value("${data.service.url:http://localhost:5003}")
    private String dataServiceUrl;

    public PveController(RoomFactory roomFactory) {
        this.roomFactory = roomFactory;
    }

    // ── Campaign lifecycle ─────────────────────────────────────────────────

    public CampaignResponse startCampaign(List<HeroRequest> heroRequests) {
        Party party = buildPartyFromRequests(heroRequests);
        Campaign campaign = new Campaign(party);
        CampaignResponse r = CampaignResponse.of(campaign, campaign.start(), null);
        r.setHeroes(heroesToRequests(party.getHeroes()));
        r.setInventory(party.getInventory());
        return r;
    }

    public CampaignResponse nextRoom(CampaignStateRequest state) {
        Party    party    = buildPartyFromState(state);
        Campaign campaign = new Campaign(party);
        campaign.setCurrentRoom(state.getCurrentRoom());

        if (campaign.isComplete())
            return CampaignResponse.error("Campaign complete! Calculate your score.");

        campaign.advanceRoom();
        Room room = roomFactory.generateRoom(campaign.getCurrentRoom(),
                                             party.getCumulativeLevel());
        String roomDescription = room.enter(party);
        String roomType        = room instanceof BattleRoom ? "BATTLE" : "INN";

        CampaignResponse response = CampaignResponse.of(campaign, roomDescription, roomType);
        response.setHeroes(heroesToRequests(party.getHeroes()));
        response.setInventory(party.getInventory());

        if (room instanceof BattleRoom battleRoom) {
            int expReward  = battleRoom.calculateTotalExp();
            int goldReward = battleRoom.calculateTotalGold();
            response.setExpReward(expReward);
            response.setGoldReward(goldReward);
            response.setEnemies(battleRoom.getEnemies());

            String battleId = "pve-" + state.getUserId() + "-room-" + campaign.getCurrentRoom();
            try {
                Map<String, Object> req = buildBattleRequest(party, battleRoom.getEnemies(), battleId);
                restTemplate.postForObject(battleServiceUrl + "/api/battle/" + battleId + "/start",
                                           req, Map.class);
            } catch (Exception e) {
                System.err.println("Warning: could not pre-register battle: " + e.getMessage());
            }
            response.setBattleId(battleId);
        }
        return response;
    }

    public CampaignResponse resolveBattle(CampaignStateRequest state,
                                           boolean playerWon, int expReward, int goldReward) {
        Party    party    = buildPartyFromState(state);
        Campaign campaign = new Campaign(party);
        campaign.setCurrentRoom(state.getCurrentRoom());
        StringBuilder msg = new StringBuilder();

        if (playerWon) {
            party.addGold(goldReward);
            List<Hero> living = party.getHeroes().stream().filter(h -> h.getHp() > 0).toList();
            if (!living.isEmpty()) {
                int perHero = expReward / living.size();
                for (Hero h : living) {
                    int old = h.getLevel();
                    h.gainExperience(perHero);
                    if (h.getLevel() > old)
                        msg.append(h.getName()).append(" levelled up to ").append(h.getLevel()).append("! ");
                }
            }
            msg.insert(0, "Victory! Gained " + goldReward + "g and " + expReward + " XP. ");
        } else {
            int penalty = party.getGold() / 5;
            party.addGold(-penalty);
            msg.append("Defeated. Lost ").append(penalty).append("g. ");
            for (Hero h : party.getHeroes()) {
                if (h.getHp() <= 0) { h.setHp(1); msg.append(h.getName()).append(" revived at 1 HP. "); }
            }
        }

        CampaignResponse response = CampaignResponse.of(campaign, msg.toString().trim(), "BATTLE");
        response.setHeroes(heroesToRequests(party.getHeroes()));
        response.setInventory(party.getInventory());
        return response;
    }

    public CampaignResponse restoreCampaign(Long userId, String partyName) {
        try {
            String url = dataServiceUrl + "/api/data/campaign/" + userId + "?partyName=" + partyName;
            @SuppressWarnings("unchecked")
            Map<String, Object> saved = restTemplate.getForObject(url, Map.class);
            if (saved == null) return CampaignResponse.error("No saved campaign found.");

            Party party = new Party();
            party.setGold(((Number) saved.get("gold")).intValue());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> heroMaps = (List<Map<String, Object>>) saved.get("heroes");
            if (heroMaps != null) {
                for (Map<String, Object> m : heroMaps) {
                    Hero h = new Hero((String) m.get("name"), (String) m.get("heroClass"));
                    h.setLevel(((Number) m.get("level")).intValue());
                    h.setAttack(((Number) m.get("attack")).intValue());
                    h.setDefense(((Number) m.get("defense")).intValue());
                    h.setHp(((Number) m.get("hp")).intValue());
                    h.setMaxHp(((Number) m.get("maxHp")).intValue());
                    h.setMana(((Number) m.get("mana")).intValue());
                    h.setMaxMana(((Number) m.get("maxMana")).intValue());
                    h.setExperience(m.containsKey("experience") ? ((Number) m.get("experience")).intValue() : 0);
                    party.addHero(h);
                }
            }

            Campaign campaign = new Campaign(party);
            campaign.setCurrentRoom(((Number) saved.get("currentRoom")).intValue());

            CampaignResponse response = CampaignResponse.of(campaign, "Campaign restored.", null);
            response.setHeroes(heroesToRequests(party.getHeroes()));
            response.setInventory(party.getInventory());
            return response;
        } catch (Exception e) {
            return CampaignResponse.error("Failed to load campaign: " + e.getMessage());
        }
    }

    public CampaignResponse saveCampaign(Long userId, String partyName,
                                          CampaignStateRequest state) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId",      userId);
            payload.put("partyName",   partyName);
            payload.put("currentRoom", state.getCurrentRoom());
            payload.put("gold",        state.getGold() != null ? state.getGold() : 0);
            payload.put("inventory",   state.getInventory() != null ? state.getInventory() : Map.of());
            payload.put("heroes", state.getHeroes() == null ? List.of() :
                state.getHeroes().stream().map(h -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name",       h.getName());
                    m.put("heroClass",  h.getHeroClass());
                    m.put("level",      h.getLevel());
                    m.put("attack",     h.getAttack());
                    m.put("defense",    h.getDefense());
                    m.put("hp",         h.getHp());
                    m.put("maxHp",      h.getMaxHp());
                    m.put("mana",       h.getMana());
                    m.put("maxMana",    h.getMaxMana());
                    m.put("experience", h.getExperience());
                    return m;
                }).toList());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(dataServiceUrl + "/api/data/campaign/save",
                    new HttpEntity<>(payload, headers), Map.class);
            return CampaignResponse.success("Campaign saved successfully.");
        } catch (Exception e) {
            return CampaignResponse.error("Failed to save: " + e.getMessage());
        }
    }

    public int calculateScore(CampaignStateRequest state) {
        Party party = buildPartyFromState(state);
        return party.getHeroes().stream().mapToInt(h -> h.getLevel() * 100).sum()
             + party.getGold() * 10;
    }

    // ── Inn actions ────────────────────────────────────────────────────────

    public CampaignResponse buyItem(CampaignStateRequest state, String itemName) {
        Party party = buildPartyFromState(state);
        Campaign campaign = new Campaign(party);
        campaign.setCurrentRoom(state.getCurrentRoom());
        Object[] found = findItem(itemName);
        if (found == null) return CampaignResponse.error("Item '" + itemName + "' not sold here.");
        int cost = (int) found[1];
        if (!party.deductGold(cost))
            return CampaignResponse.error("Not enough gold. Need " + cost + "g, have " + party.getGold() + "g.");
        party.addItem(itemName, 1);
        CampaignResponse response = CampaignResponse.of(campaign,
                "Bought " + itemName + " for " + cost + "g. Gold: " + party.getGold() + "g.", "INN");
        response.setHeroes(heroesToRequests(party.getHeroes()));
        response.setInventory(party.getInventory());
        return response;
    }

    public CampaignResponse recruitHero(CampaignStateRequest state,
                                         String heroName, String heroClass) {
        Party party = buildPartyFromState(state);
        Campaign campaign = new Campaign(party);
        campaign.setCurrentRoom(state.getCurrentRoom());
        if (party.getHeroes().size() >= 5)
            return CampaignResponse.error("Party full — max 5 heroes.");
        int cost = 300 + 50 * campaign.getCurrentRoom();
        if (!party.deductGold(cost))
            return CampaignResponse.error("Not enough gold. Need " + cost + "g, have " + party.getGold() + "g.");
        party.addHero(new Hero(heroName, heroClass));
        CampaignResponse response = CampaignResponse.of(campaign,
                "Recruited " + heroName + " [" + heroClass + "] for " + cost + "g.", "INN");
        response.setHeroes(heroesToRequests(party.getHeroes()));
        response.setInventory(party.getInventory());
        return response;
    }

    public CampaignResponse useItem(CampaignStateRequest state, String itemName, int heroIndex) {
        Party party = buildPartyFromState(state);
        Campaign campaign = new Campaign(party);
        campaign.setCurrentRoom(state.getCurrentRoom());
        if (heroIndex < 0 || heroIndex >= party.getHeroes().size())
            return CampaignResponse.error("Invalid hero index: " + heroIndex);
        if (!party.useItem(itemName))
            return CampaignResponse.error("Item '" + itemName + "' not in inventory.");
        Hero hero = party.getHeroes().get(heroIndex);
        Object[] found = findItem(itemName);
        StringBuilder effect = new StringBuilder(hero.getName() + " used " + itemName + ". ");
        if (found != null) {
            applyItemEffect(hero, (int)found[2], (int)found[3], (int)found[4], (int)found[5], effect);
        }
        CampaignResponse response = CampaignResponse.of(campaign, effect.toString().trim(), "INN");
        response.setHeroes(heroesToRequests(party.getHeroes()));
        response.setInventory(party.getInventory());
        return response;
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private Party buildPartyFromRequests(List<HeroRequest> heroRequests) {
        Party party = new Party();
        if (heroRequests != null) {
            for (HeroRequest req : heroRequests) party.addHero(new Hero(req.getName(), req.getHeroClass()));
        }
        return party;
    }

    private Party buildPartyFromState(CampaignStateRequest state) {
        Party party = new Party();
        party.setGold(state.getGold() != null ? state.getGold() : 0);
        if (state.getInventory() != null) party.setInventory(state.getInventory());
        if (state.getHeroes() != null) {
            for (HeroRequest hr : state.getHeroes()) {
                Hero h = new Hero(hr.getName(), hr.getHeroClass());
                h.setLevel(hr.getLevel()); h.setAttack(hr.getAttack()); h.setDefense(hr.getDefense());
                h.setHp(hr.getHp()); h.setMaxHp(hr.getMaxHp());
                h.setMana(hr.getMana()); h.setMaxMana(hr.getMaxMana());
                h.setExperience(hr.getExperience());
                party.addHero(h);
            }
        }
        return party;
    }

    private List<HeroRequest> heroesToRequests(List<Hero> heroes) {
        return heroes.stream().map(h -> {
            HeroRequest req = new HeroRequest();
            req.setName(h.getName()); req.setHeroClass(h.getHeroClass());
            req.setLevel(h.getLevel()); req.setAttack(h.getAttack()); req.setDefense(h.getDefense());
            req.setHp(h.getHp()); req.setMaxHp(h.getMaxHp());
            req.setMana(h.getMana()); req.setMaxMana(h.getMaxMana());
            req.setExperience(h.getExperience());
            return req;
        }).toList();
    }

    private void applyItemEffect(Hero hero, int hpBonus, int manaBonus,
                                  int attackBonus, int defenseBonus, StringBuilder effect) {
        if (hpBonus > 0)      { hero.setHp(Math.min(hero.getHp() + hpBonus, hero.getMaxHp())); effect.append("+").append(hpBonus).append(" HP. "); }
        if (manaBonus > 0)    { hero.setMana(Math.min(hero.getMana() + manaBonus, hero.getMaxMana())); effect.append("+").append(manaBonus).append(" mana. "); }
        if (attackBonus > 0)  { hero.setAttack(hero.getAttack() + attackBonus); effect.append("+").append(attackBonus).append(" attack. "); }
        if (defenseBonus > 0) { hero.setDefense(hero.getDefense() + defenseBonus); effect.append("+").append(defenseBonus).append(" defense. "); }
    }

    private Object[] findItem(String itemName) {
        for (Object[] item : InnRoom.ITEMS) { if (item[0].equals(itemName)) return item; }
        return null;
    }

    private Map<String, Object> buildBattleRequest(Party party, List<Enemy> enemies, String battleId) {
        List<Map<String, Object>> playerUnits = party.getHeroes().stream().map(h -> {
            Map<String, Object> u = new HashMap<>();
            u.put("name", h.getName()); u.put("level", h.getLevel());
            u.put("attack", h.getAttack()); u.put("defense", h.getDefense());
            u.put("hp", h.getHp()); u.put("maxHp", h.getMaxHp());
            u.put("mana", h.getMana()); u.put("maxMana", h.getMaxMana());
            u.put("heroClass", h.getHeroClass()); return u;
        }).toList();
        List<Map<String, Object>> enemyUnits = enemies.stream().map(e -> {
            Map<String, Object> u = new HashMap<>();
            u.put("name", e.getName()); u.put("level", e.getLevel());
            u.put("attack", e.getAttack()); u.put("defense", e.getDefense());
            u.put("hp", e.getHp()); u.put("maxHp", e.getMaxHp());
            u.put("mana", 0); u.put("maxMana", 0); return u;
        }).toList();
        Map<String, Object> request = new HashMap<>();
        request.put("playerUnits", playerUnits);
        request.put("enemyUnits",  enemyUnits);
        return request;
    }
}
