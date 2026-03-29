package com.legends.pve.controller;

import com.legends.pve.dto.*;
import com.legends.pve.service.PveController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PvE REST API on port 5002.
 *
 * POST /api/pve/{userId}/start           — start new campaign
 * POST /api/pve/{userId}/next-room       — advance to next room
 * GET  /api/pve/{userId}/campaign        — get current campaign state
 * POST /api/pve/{userId}/restore         — restore from saved state
 * GET  /api/pve/{userId}/score           — calculate campaign score
 * POST /api/pve/{userId}/end             — end/save campaign session
 * With the use of AI
 */
@RestController
@RequestMapping("/api/pve")
public class PveCampaignController {

    private final PveController pveController;

    public PveCampaignController(PveController pveController) {
        this.pveController = pveController;
    }

    @PostMapping("/{userId}/start")
    public ResponseEntity<CampaignResponse> startCampaign(
            @PathVariable Long userId,
            @RequestBody List<HeroRequest> heroes) {
        return ResponseEntity.ok(pveController.startCampaign(userId, heroes));
    }

    @PostMapping("/{userId}/next-room")
    public ResponseEntity<CampaignResponse> nextRoom(@PathVariable Long userId) {
        CampaignResponse response = pveController.nextRoom(userId);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/{userId}/campaign")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable Long userId) {
        CampaignResponse response = pveController.getCampaign(userId);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{userId}/restore")
    public ResponseEntity<CampaignResponse> restoreCampaign(
            @PathVariable Long userId,
            @RequestBody SavedStateRequest savedState) {
        return ResponseEntity.ok(pveController.restoreCampaign(userId, savedState));
    }

    @GetMapping("/{userId}/score")
    public ResponseEntity<Integer> getScore(@PathVariable Long userId) {
        return ResponseEntity.ok(pveController.calculateScore(userId));
    }

    @PostMapping("/{userId}/end")
    public ResponseEntity<Void> endCampaign(@PathVariable Long userId) {
        pveController.endCampaign(userId);
        return ResponseEntity.ok().build();
    }
}
