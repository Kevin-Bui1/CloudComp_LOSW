package com.legends.data.controller;

import com.legends.data.dto.CampaignState;
import com.legends.data.dto.SaveRequest;
import com.legends.data.model.Party;
import com.legends.data.service.GameSaveDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final GameSaveDAO gameSaveDAO;

    public DataController(GameSaveDAO gameSaveDAO) {
        this.gameSaveDAO = gameSaveDAO;
    }

    /** Save or update a campaign's progress. */
    @PostMapping("/campaign/save")
    public ResponseEntity<Party> saveCampaign(@RequestBody SaveRequest request) {
        return ResponseEntity.ok(gameSaveDAO.saveCampaignProgress(request));
    }

    /** Load the active campaign state for a user. */
    @GetMapping("/campaign/{userId}")
    public ResponseEntity<CampaignState> loadCampaign(@PathVariable Long userId) {
        return gameSaveDAO.fetchSavedCampaign(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Mark a campaign as completed. */
    @PatchMapping("/campaign/{userId}/complete")
    public ResponseEntity<Void> completeCampaign(@PathVariable Long userId) {
        gameSaveDAO.completeCampaign(userId);
        return ResponseEntity.ok().build();
    }

    /** Delete a saved party */
    @DeleteMapping("/party/{partyId}")
    public ResponseEntity<Void> deleteParty(@PathVariable Long partyId) {
        gameSaveDAO.deleteParty(partyId);
        return ResponseEntity.ok().build();
    }

    /** List all saved parties for a user. */
    @GetMapping("/parties/{userId}")
    public ResponseEntity<List<Party>> getSavedParties(@PathVariable Long userId) {
        return ResponseEntity.ok(gameSaveDAO.getSavedParties(userId));
    }
}
