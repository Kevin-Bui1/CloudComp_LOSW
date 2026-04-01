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
 * STATELESS CONTRACT:
 *   Every mutating endpoint (nextRoom, resolveBattle, inn actions) requires
 *   the client to send the full CampaignStateRequest body containing the
 *   current heroes, gold, currentRoom, and inventory. The service reconstructs
 *   state from the request, applies the operation, and returns the updated
 *   CampaignResponse — which the client must store and echo back next time.
 *
 * Campaign lifecycle:
 *   POST /api/pve/{userId}/start           — start new campaign (body: hero list)
 *   POST /api/pve/{userId}/next-room       — advance to next room (body: CampaignStateRequest)
 *   POST /api/pve/{userId}/battle/resolve  — apply battle outcome (body: BattleResolveRequest)
 *   POST /api/pve/{userId}/save            — save to data-service (body: SaveCampaignRequest)
 *   POST /api/pve/{userId}/restore         — load from data-service (body: {partyName})
 *   GET  /api/pve/{userId}/score           — calculate score (body: CampaignStateRequest)
 *
 * Inn actions (must send CampaignStateRequest as body):
 *   POST /api/pve/{userId}/inn/buy         — { state + "itemName" }
 *   POST /api/pve/{userId}/inn/recruit     — { state + "heroName", "heroClass" }
 *   POST /api/pve/{userId}/inn/use-item    — { state + "itemName", "heroIndex" }
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
        return ResponseEntity.ok(pveController.startCampaign(heroes));
    }

    @PostMapping("/{userId}/next-room")
    public ResponseEntity<CampaignResponse> nextRoom(
            @PathVariable Long userId,
            @RequestBody CampaignStateRequest state) {
        state.setUserId(userId);
        CampaignResponse response = pveController.nextRoom(state);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{userId}/battle/resolve")
    public ResponseEntity<CampaignResponse> resolveBattle(
            @PathVariable Long userId,
            @RequestBody BattleResolveRequest req) {
        req.getState().setUserId(userId);
        CampaignResponse response = pveController.resolveBattle(
                req.getState(), req.isPlayerWon(), req.getExpReward(), req.getGoldReward());
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{userId}/save")
    public ResponseEntity<CampaignResponse> saveCampaign(
            @PathVariable Long userId,
            @RequestBody SaveCampaignRequest req) {
        CampaignResponse response = pveController.saveCampaign(userId, req.getPartyName(), req.getState());
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.internalServerError().body(response);
    }

    @PostMapping("/{userId}/restore")
    public ResponseEntity<CampaignResponse> restoreCampaign(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String partyName = body.get("partyName");
        if (partyName == null || partyName.isBlank())
            return ResponseEntity.badRequest().body(CampaignResponse.error("partyName is required."));
        CampaignResponse response = pveController.restoreCampaign(userId, partyName);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{userId}/score")
    public ResponseEntity<Map<String, Integer>> getScore(
            @PathVariable Long userId,
            @RequestBody CampaignStateRequest state) {
        state.setUserId(userId);
        return ResponseEntity.ok(Map.of("score", pveController.calculateScore(state)));
    }

    // ── Inn actions (UC4) ─────────────────────────────────────────────────

    @PostMapping("/{userId}/inn/buy")
    public ResponseEntity<CampaignResponse> buyItem(
            @PathVariable Long userId,
            @RequestBody InnActionRequest req) {
        req.getState().setUserId(userId);
        if (req.getItemName() == null || req.getItemName().isBlank())
            return ResponseEntity.badRequest().body(CampaignResponse.error("itemName is required."));
        CampaignResponse response = pveController.buyItem(req.getState(), req.getItemName());
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{userId}/inn/recruit")
    public ResponseEntity<CampaignResponse> recruitHero(
            @PathVariable Long userId,
            @RequestBody InnActionRequest req) {
        req.getState().setUserId(userId);
        if (req.getHeroName() == null || req.getHeroClass() == null)
            return ResponseEntity.badRequest().body(CampaignResponse.error("heroName and heroClass are required."));
        CampaignResponse response = pveController.recruitHero(req.getState(), req.getHeroName(), req.getHeroClass());
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{userId}/inn/use-item")
    public ResponseEntity<CampaignResponse> useItem(
            @PathVariable Long userId,
            @RequestBody InnActionRequest req) {
        req.getState().setUserId(userId);
        if (req.getItemName() == null || req.getHeroIndex() == null)
            return ResponseEntity.badRequest().body(CampaignResponse.error("itemName and heroIndex are required."));
        CampaignResponse response = pveController.useItem(req.getState(), req.getItemName(), req.getHeroIndex());
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}
