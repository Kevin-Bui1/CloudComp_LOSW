package com.legends.pve;

import com.legends.pve.dto.CampaignResponse;
import com.legends.pve.dto.HeroRequest;
import com.legends.pve.model.*;
import com.legends.pve.service.PveController;
import com.legends.pve.service.RoomFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the PvE Service.
 *
 * These are plain unit tests — no Spring context needed since PveController
 * is just a regular Java class (not database-backed). I create it directly
 * with a real RoomFactory so the full campaign logic runs.
 */
class PveServiceTests {

    private PveController pveController;

    @BeforeEach
    void setUp() {
        pveController = new PveController(new RoomFactory());
    }

    // PVE-TC-01: Starting a campaign should create a party and begin at room 0
    @Test
    void testStartCampaign_createsPartyWithHeroes() {
        HeroRequest h1 = hero("Arthur", "WARRIOR");
        HeroRequest h2 = hero("Merlin", "MAGE");

        CampaignResponse response = pveController.startCampaign(1L, List.of(h1, h2));

        assertTrue(response.isSuccess(), "Campaign should start successfully");
        CampaignResponse campaignInfo = pveController.getCampaign(1L);
        assertNotNull(campaignInfo, "Campaign should be retrievable after start");
        assertEquals(0, campaignInfo.getCurrentRoom(), "Room should start at 0");
    }

    // PVE-TC-02: Calling nextRoom should increment the room counter
    @Test
    void testNextRoom_advancesRoomCounter() {
        pveController.startCampaign(2L, List.of(hero("Hero", "CHAOS")));
        pveController.nextRoom(2L);

        CampaignResponse state = pveController.getCampaign(2L);
        assertEquals(1, state.getCurrentRoom(), "Room counter should advance to 1");
    }

    // PVE-TC-03: Every room should be either BATTLE or INN (no other types)
    @Test
    void testNextRoom_returnsBattleOrInn() {
        pveController.startCampaign(3L, List.of(hero("Hero", "ORDER")));
        CampaignResponse response = pveController.nextRoom(3L);

        assertTrue(response.getRoomType().equals("BATTLE") || response.getRoomType().equals("INN"),
                "Room type must be BATTLE or INN");
    }

    // PVE-TC-04: Asking for a campaign that doesn't exist should return an error
    @Test
    void testGetCampaign_unknownUser_returnsError() {
        CampaignResponse response = pveController.getCampaign(999L);
        assertFalse(response.isSuccess(), "Should return error for unknown user");
    }

    // PVE-TC-05: Score = (hero level * 100) + (gold * 10) — test with level 1 hero, 0 gold
    @Test
    void testCalculateScore_correctFormula() {
        pveController.startCampaign(4L, List.of(hero("Hero", "WARRIOR")));
        int score = pveController.calculateScore(4L);
        assertEquals(100, score, "Score should be 100 for level 1 hero with no gold");
    }

    // PVE-TC-06: Hero should gain levels when XP reaches the threshold
    @Test
    void testHero_gainExperience_levelsUp() {
        Hero hero = new Hero("Ares", "WARRIOR");
        int expNeeded = hero.expToNextLevel();
        hero.gainExperience(expNeeded);
        assertEquals(2, hero.getLevel(), "Hero should reach level 2 after enough XP");
    }

    // PVE-TC-07: Entering an InnRoom should restore HP and mana to full
    @Test
    void testInnRoom_healsAllHeroes() {
        Hero hero = new Hero("Healer", "ORDER");
        hero.setHp(50); // simulate taking damage
        Party party = new Party();
        party.addHero(hero);
        InnRoom inn = new InnRoom(5);
        inn.enter(party);
        assertEquals(hero.getMaxHp(), hero.getHp(), "Hero should be fully healed after inn");
        assertEquals(hero.getMaxMana(), hero.getMana(), "Hero mana should be fully restored");
    }

    // PVE-TC-08: BattleRoom rewards should match the formulas: Exp=50*L, Gold=75*L
    @Test
    void testBattleRoom_rewardsCalculated() {
        Enemy enemy = new Enemy("Goblin", 3, 10, 5, 60, 10);
        BattleRoom room = new BattleRoom(2, List.of(enemy));
        assertEquals(150, room.calculateTotalExp(),  "Exp = 50 * level = 150 for level 3 enemy");
        assertEquals(225, room.calculateTotalGold(), "Gold = 75 * level = 225 for level 3 enemy");
    }

    // PVE-TC-09: getCumulativeLevel should add up all hero levels correctly
    @Test
    void testParty_cumulativeLevelCorrect() {
        Party party = new Party();
        Hero h1 = new Hero("A", "WARRIOR"); h1.setLevel(3);
        Hero h2 = new Hero("B", "MAGE");    h2.setLevel(5);
        party.addHero(h1);
        party.addHero(h2);
        assertEquals(8, party.getCumulativeLevel(), "Cumulative level should be 3+5=8");
    }

    // PVE-TC-10: Restoring a campaign should bring back the exact saved room and gold
    @Test
    void testRestoreCampaign_correctlyRestoresState() {
        com.legends.pve.dto.SavedStateRequest state = new com.legends.pve.dto.SavedStateRequest();
        state.setCurrentRoom(15);
        state.setGold(750);
        state.setHeroes(List.of(hero("Restored", "CHAOS")));

        CampaignResponse response = pveController.restoreCampaign(5L, state);

        assertTrue(response.isSuccess());
        assertEquals(15, response.getCurrentRoom(), "Restored campaign should be at room 15");
        assertEquals(750, response.getGold(),       "Restored campaign should have 750 gold");
    }

    // -- Helper --------------------------------------------------------------

    private HeroRequest hero(String name, String heroClass) {
        HeroRequest h = new HeroRequest();
        h.setName(name);
        h.setHeroClass(heroClass);
        return h;
    }
}
