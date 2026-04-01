package com.legends.data.controller;

import com.legends.data.dto.CampaignState;
import com.legends.data.dto.SaveRequest;
import com.legends.data.model.Party;
import com.legends.data.service.GameSaveDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Data Service REST API on port 5003.
 *
 * Campaign:
 *   POST   /api/data/campaign/save          — save/update campaign progress
 *   GET    /api/data/campaign/{userId}       — load active campaign
 *   PATCH  /api/data/campaign/{userId}/complete — mark campaign finished
 *   DELETE /api/data/party/{partyId}         — delete a saved party
 *   GET    /api/data/parties/{userId}        — list all parties for a user
 *
 * Scores:
 *   POST   /api/data/scores/{userId}         — record a campaign score
 *   GET    /api/data/scores/{userId}/best    — get user's best score
 *   GET    /api/data/scores/top              — get leaderboard (top 10)
 *
 * PvP:
 *   POST   /api/data/pvp/result             — record a PvP win/loss
 *   GET    /api/data/pvp/{userId}/record    — get win/loss counts
 *
 * Player helpers (used by pvp-service for invite validation):
 *   GET    /api/data/players/{username}/eligible          — exists + has parties?
 *   GET    /api/data/players/{username}/parties/{name}/heroes — heroes in a party
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    private final GameSaveDAO gameSaveDAO;

    public DataController(GameSaveDAO gameSaveDAO) {
        this.gameSaveDAO = gameSaveDAO;
    }

    // ── Campaign ─────────────────────────────────────────────────────────

    @PostMapping("/campaign/save")
    public ResponseEntity<Party> saveCampaign(@RequestBody SaveRequest request) {
        return ResponseEntity.ok(gameSaveDAO.saveCampaignProgress(request));
    }

    @GetMapping("/campaign/{userId}")
    public ResponseEntity<CampaignState> loadCampaign(@PathVariable Long userId) {
        return gameSaveDAO.fetchSavedCampaign(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/campaign/{userId}/complete")
    public ResponseEntity<Void> completeCampaign(@PathVariable Long userId) {
        gameSaveDAO.completeCampaign(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/party/{partyId}")
    public ResponseEntity<Void> deleteParty(@PathVariable Long partyId) {
        gameSaveDAO.deleteParty(partyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/parties/{userId}")
    public ResponseEntity<List<Party>> getSavedParties(@PathVariable Long userId) {
        return ResponseEntity.ok(gameSaveDAO.getSavedParties(userId));
    }

    // ── Scores ───────────────────────────────────────────────────────────

    @PostMapping("/scores/{userId}")
    public ResponseEntity<Void> saveScore(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        int score        = body.get("score") instanceof Number n ? n.intValue() : 0;
        String username  = body.get("username") instanceof String s ? s : "user-" + userId;
        gameSaveDAO.saveScore(userId, username, score);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/scores/{userId}/best")
    public ResponseEntity<Map<String, Object>> getBestScore(@PathVariable Long userId) {
        int best = gameSaveDAO.getBestScore(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "bestScore", best));
    }

    @GetMapping("/scores/top")
    public ResponseEntity<List<Map<String, Object>>> getTopScores(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(gameSaveDAO.getTopScores(limit));
    }

    // ── PvP ──────────────────────────────────────────────────────────────

    @PostMapping("/pvp/result")
    public ResponseEntity<Void> recordPvpResult(@RequestBody Map<String, String> body) {
        String winner = body.get("winnerUsername");
        String loser  = body.get("loserUsername");
        if (winner == null || loser == null)
            return ResponseEntity.badRequest().build();
        gameSaveDAO.recordPvpResult(winner, loser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pvp/{userId}/record")
    public ResponseEntity<Map<String, Object>> getPvpRecord(@PathVariable Long userId) {
        int[] record = gameSaveDAO.getPvpRecord(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "wins",   record[0],
                "losses", record[1]
        ));
    }

    // ── Player helpers (used by pvp-service) ────────────────────────────

    /**
     * Returns { eligible: true/false } — used by pvp-service to validate
     * that a player exists and has at least one saved party before an invite.
     */
    @GetMapping("/players/{username}/eligible")
    public ResponseEntity<Map<String, Object>> isPlayerEligible(@PathVariable String username) {
        boolean eligible = gameSaveDAO.isPlayerEligible(username);
        return ResponseEntity.ok(Map.of("username", username, "eligible", eligible));
    }

    /**
     * Returns the heroes for a specific named party — used by pvp-service
     * to load both parties before submitting them to the battle-service.
     */
    @GetMapping("/players/{username}/parties/{partyName}/heroes")
    public ResponseEntity<?> getPartyHeroes(
            @PathVariable String username,
            @PathVariable String partyName) {
        var heroes = gameSaveDAO.getPartyHeroes(username, partyName);
        if (heroes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(heroes);
    }
}
