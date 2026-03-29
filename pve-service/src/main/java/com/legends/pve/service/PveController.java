package com.legends.pve.service;

import com.legends.pve.dto.*;
import com.legends.pve.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PveController is the main service class for the PvE campaign.
 * (The name is a bit confusing — it's a @Service not an HTTP controller.
 * The actual HTTP controller is PveCampaignController which calls this.)
 *
 * This class manages all active campaign sessions. Each user gets their own
 * Campaign object stored in a ConcurrentHashMap (same thread-safety reason
 * as in BattleService — multiple users can be playing at the same time).
 *
 * The campaign flow is:
 *   startCampaign → nextRoom (x30) → calculateScore → endCampaign
 *
 * restoreCampaign is the "continue" path — loads a saved state from the
 * Data Service and rebuilds a Campaign object from it.
 */
@Service
public class PveController {

    private final RoomFactory roomFactory;

    /** One Campaign per logged-in user, keyed by userId. */
    private final Map<Long, Campaign> activeCampaigns = new ConcurrentHashMap<>();

    public PveController(RoomFactory roomFactory) {
        this.roomFactory = roomFactory;
    }

    /**
     * Starts a new campaign for the user with the given heroes.
     * Creates fresh Hero objects from the request and puts them in a Party.
     */
    public CampaignResponse startCampaign(Long userId, List<HeroRequest> heroRequests) {
        Party party = new Party();
        for (HeroRequest req : heroRequests) {
            Hero hero = new Hero(req.getName(), req.getHeroClass());
            party.addHero(hero);
        }
        Campaign campaign = new Campaign(party);
        activeCampaigns.put(userId, campaign);
        String message = campaign.start();
        return CampaignResponse.of(campaign, message, null);
    }

    /**
     * Moves the party into the next room and returns what's in it.
     *
     * The RoomFactory decides if it's a battle or an inn based on probability.
     * For battle rooms, I include the enemy list and the exp/gold rewards in the response
     * so the client knows what it's fighting for before the battle starts.
     */
    public CampaignResponse nextRoom(Long userId) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return CampaignResponse.error("No active campaign found.");
        if (campaign.isComplete()) return CampaignResponse.error("Campaign complete! Check your score.");

        campaign.advanceRoom();
        Room room = roomFactory.generateRoom(
                campaign.getCurrentRoom(),
                campaign.getParty().getCumulativeLevel()
        );

        String roomDescription = room.enter(campaign.getParty());
        String roomType = room instanceof BattleRoom ? "BATTLE" : "INN";

        CampaignResponse response = CampaignResponse.of(campaign, roomDescription, roomType);
        if (room instanceof BattleRoom battleRoom) {
            response.setEnemies(battleRoom.getEnemies());
            response.setExpReward(battleRoom.calculateTotalExp());
            response.setGoldReward(battleRoom.calculateTotalGold());
        }
        return response;
    }

    /**
     * Returns the current campaign state without advancing to a new room.
     * Used when the player wants to review their party mid-campaign.
     */
    public CampaignResponse getCampaign(Long userId) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return CampaignResponse.error("No active campaign.");
        return CampaignResponse.of(campaign, "Campaign info retrieved.", null);
    }

    /**
     * Rebuilds an in-memory Campaign from a saved state loaded from the Data Service.
     * This is UC6 — the player exits and comes back later.
     *
     * All hero stats (level, HP, attack, etc.) are restored exactly as saved.
     */
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

    /**
     * Calculates the final score at the end of a 30-room campaign.
     *
     * Formula from the assignment:
     *   (sum of hero levels * 100) + (gold * 10)
     */
    public int calculateScore(Long userId) {
        Campaign campaign = activeCampaigns.get(userId);
        if (campaign == null) return 0;
        Party party = campaign.getParty();
        int levelScore = party.getHeroes().stream().mapToInt(h -> h.getLevel() * 100).sum();
        int goldScore  = party.getGold() * 10;
        return levelScore + goldScore;
    }

    /**
     * Removes the campaign session from memory.
     * Called after the player saves (so the Data Service has it) or after score is recorded.
     */
    public void endCampaign(Long userId) {
        activeCampaigns.remove(userId);
    }
}
