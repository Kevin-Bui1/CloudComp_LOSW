package com.legends.data;

import com.legends.data.dto.HeroDTO;
import com.legends.data.dto.SaveRequest;
import com.legends.data.dto.CampaignState;
import com.legends.data.model.Party;
import com.legends.data.service.GameSaveDAO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Data Service (GameSaveDAO).
 *
 * These run against an in-memory H2 database (set up by application-test.properties)
 * so I don't need a real MySQL instance to run the tests.
 *
 * @Transactional rolls back after each test, so test data doesn't bleed between tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataServiceTests {

    @Autowired
    private GameSaveDAO gameSaveDAO;

    // DB-TC-01: Saving a campaign should persist the party and all heroes
    @Test
    void testSaveCampaignProgress_savesPartyAndHeroes() {
        SaveRequest req = buildSaveRequest(1L, "The Round Table", 12, 500);
        Party saved = gameSaveDAO.saveCampaignProgress(req);

        assertNotNull(saved.getPartyId(), "Party should have a generated ID after save");
        assertEquals(12,  saved.getCurrentRoom(), "Current room should be saved as 12");
        assertEquals(500, saved.getGold(),        "Gold should be saved as 500");
        assertEquals(2,   saved.getHeroes().size(), "Both heroes should be persisted");
    }

    // DB-TC-02: Loading a saved campaign should reconstruct all party and hero data (UC6)
    @Test
    void testFetchSavedCampaign_reconstructsPartyAndHeroes() {
        gameSaveDAO.saveCampaignProgress(buildSaveRequest(2L, "Iron Dawn", 7, 250));
        Optional<CampaignState> result = gameSaveDAO.fetchSavedCampaign(2L);

        assertTrue(result.isPresent(), "Campaign state should be present for user 2");
        CampaignState state = result.get();
        assertEquals(7,   state.getCurrentRoom());
        assertEquals(250, state.getGold());
        assertEquals(2,   state.getHeroes().size());
        assertEquals("Arthur", state.getHeroes().get(0).getName());
    }

    // DB-TC-03: Saving again should update the existing record, not create a duplicate
    @Test
    void testSaveCampaign_updateExisting_noDuplicate() {
        gameSaveDAO.saveCampaignProgress(buildSaveRequest(3L, "First Save", 3, 100));
        gameSaveDAO.saveCampaignProgress(buildSaveRequest(3L, "Updated Save", 6, 200));

        assertEquals(1, gameSaveDAO.countSavedParties(3L),
                "Re-saving should update the existing party, not create a new one");
    }

    // DB-TC-04: Looking up a campaign for a user who has never saved should return empty
    @Test
    void testFetchSavedCampaign_noSave_returnsEmpty() {
        Optional<CampaignState> result = gameSaveDAO.fetchSavedCampaign(999L);
        assertFalse(result.isPresent(), "Should return empty for user with no save");
    }

    // DB-TC-05: completeCampaign should mark the campaign inactive so it no longer appears as active
    @Test
    void testCompleteCampaign_marksInactive() {
        gameSaveDAO.saveCampaignProgress(buildSaveRequest(4L, "Finished Run", 30, 1000));
        gameSaveDAO.completeCampaign(4L);
        Optional<CampaignState> result = gameSaveDAO.fetchSavedCampaign(4L);
        assertFalse(result.isPresent(),
                "Completed campaign should no longer appear as active");
    }

    // DB-TC-06: Individual hero stats (level, attack, etc.) should be saved and loaded correctly
    @Test
    void testSave_heroStatsPersistedCorrectly() {
        SaveRequest req = buildSaveRequest(5L, "Stats Test", 5, 300);
        gameSaveDAO.saveCampaignProgress(req);
        CampaignState state = gameSaveDAO.fetchSavedCampaign(5L).orElseThrow();

        HeroDTO hero = state.getHeroes().get(0);
        assertEquals("Arthur", hero.getName());
        assertEquals(5,        hero.getLevel());
        assertEquals(20,       hero.getAttack());
        assertEquals(10,       hero.getDefense());
    }

    // -- Test helper ---------------------------------------------------------

    /**
     * Builds a SaveRequest with two heroes (Arthur and Merlin) for reuse across tests.
     * Centralizing this here means I only have to update it in one place if the DTO changes.
     */
    private SaveRequest buildSaveRequest(Long userId, String name, int room, int gold) {
        HeroDTO h1 = new HeroDTO();
        h1.setName("Arthur"); h1.setLevel(5); h1.setAttack(20); h1.setDefense(10);
        h1.setHp(80); h1.setMaxHp(100); h1.setMana(40); h1.setMaxMana(80);
        h1.setHeroClass("WARRIOR");

        HeroDTO h2 = new HeroDTO();
        h2.setName("Merlin"); h2.setLevel(4); h2.setAttack(10); h2.setDefense(5);
        h2.setHp(60); h2.setMaxHp(90); h2.setMana(70); h2.setMaxMana(100);
        h2.setHeroClass("MAGE");

        SaveRequest req = new SaveRequest();
        req.setUserId(userId);
        req.setPartyName(name);
        req.setCurrentRoom(room);
        req.setGold(gold);
        req.setHeroes(List.of(h1, h2));
        return req;
    }
}
