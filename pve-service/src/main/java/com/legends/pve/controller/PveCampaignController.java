package com.legends.pve.controller;

import com.legends.pve.dto.*;
import com.legends.pve.service.PveController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PvE REST API on port 5002.
 *
 * Campaign lifecycle:
 *   POST /api/pve/{userId}/start           — start new campaign
 *   POST /api/pve/{userId}/next-room       — advance to next room
 *   GET  /api/pve/{userId}/campaign        — get current campaign state
 *   POST /api/pve/{userId}/restore         — restore from saved state
 *   GET  /api/pve/{userId}/score           — calculate campaign score
 *   POST /api/pve/{userId}/end             — end/save campaign session
 *
 * Inn actions (UC4 — must be in an inn room before calling these):
 *   POST /api/pve/{userId}/inn/buy         — buy item: { "itemName": "Bread" }
 *   POST /api/pve/{userId}/inn/recruit     — recruit hero: { "heroName": "...", "heroClass": "..." }
 *   POST /api/pve/{userId}/inn/use-item    — use item on hero: { "itemName": "...", "heroIndex": 0 }
 */
@RestController
@RequestMapping("/api/pve")
public class PveCampaignController {

    private final PveController pveController;

    public PveCampaignController(PveController pveController) {
        this.pveController = pveController;
    }

    // ── Campaign lifecycle ────────────────────────────────────────────────

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

    /**
     * POST /api/pve/{userId}/battle/resolve
     * Body: { "playerWon": true }
     *
     * Called by the client after the battle-service reports a battle has ended.
     * Applies XP and gold rewards (win) or gold penalty + revive (loss) to the party.
     */
    @PostMapping("/{userId}/battle/resolve")
    public ResponseEntity<CampaignResponse> resolveBattle(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        boolean playerWon = Boolean.TRUE.equals(body.get("playerWon"));
        CampaignResponse response = pveController.resolveBattle(userId, playerWon);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    // ── Inn actions (UC4) ─────────────────────────────────────────────────

    /**
     * POST /api/pve/{userId}/inn/buy
     * Body: { "itemName": "Bread" }
     */
    @PostMapping("/{userId}/inn/buy")
    public ResponseEntity<CampaignResponse> buyItem(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String itemName = body.get("itemName");
        if (itemName == null || itemName.isBlank())
            return ResponseEntity.badRequest().body(CampaignResponse.error("itemName is required."));
        CampaignResponse response = pveController.buyItem(userId, itemName);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * POST /api/pve/{userId}/inn/recruit
     * Body: { "heroName": "Aldric", "heroClass": "WARRIOR" }
     */
    @PostMapping("/{userId}/inn/recruit")
    public ResponseEntity<CampaignResponse> recruitHero(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String heroName  = body.get("heroName");
        String heroClass = body.get("heroClass");
        if (heroName == null || heroClass == null)
            return ResponseEntity.badRequest().body(CampaignResponse.error("heroName and heroClass are required."));
        CampaignResponse response = pveController.recruitHero(userId, heroName, heroClass);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * POST /api/pve/{userId}/inn/use-item
     * Body: { "itemName": "Bread", "heroIndex": 0 }
     */
    @PostMapping("/{userId}/inn/use-item")
    public ResponseEntity<CampaignResponse> useItem(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        String itemName  = (String) body.get("itemName");
        Integer heroIndex = body.get("heroIndex") instanceof Number n ? n.intValue() : null;
        if (itemName == null || heroIndex == null)
            return ResponseEntity.badRequest().body(CampaignResponse.error("itemName and heroIndex are required."));
        CampaignResponse response = pveController.useItem(userId, itemName, heroIndex);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}
