package com.legends.battle.controller;

import com.legends.battle.dto.BattleRequest;
import com.legends.battle.dto.BattleResponse;
import com.legends.battle.model.Action;
import com.legends.battle.service.BattleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * BattleController exposes the combat system as a REST API on port 5001.
 *
 * There are only two endpoints:
 *   POST /api/battle/{battleId}/start  — kick off a new battle with two parties
 *   POST /api/battle/{battleId}/action — execute one action for the current unit
 *
 * The battleId in the URL is just a string key so we can run multiple battles
 * at once without them mixing up. The PvE service passes something like "user1_battle".
 */
@RestController
@RequestMapping("/api/battle")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    /**
     * POST /api/battle/{battleId}/start
     *
     * Receives both parties as JSON, creates a battle session, and returns the
     * initial state (who goes first, everyone's HP, etc.).
     */
    @PostMapping("/{battleId}/start")
    public ResponseEntity<BattleResponse> startBattle(
            @PathVariable String battleId,
            @RequestBody BattleRequest request) {
        return ResponseEntity.ok(battleService.startBattle(battleId, request));
    }

    /**
     * POST /api/battle/{battleId}/action?action=ATTACK
     *
     * Executes one action for whoever's turn it currently is.
     * The action comes in as a query parameter (ATTACK, DEFEND, WAIT, or CAST).
     * Returns the updated party states and whether the battle is over.
     */
    @PostMapping("/{battleId}/action")
    public ResponseEntity<BattleResponse> executeAction(
            @PathVariable String battleId,
            @RequestParam Action action) {
        return ResponseEntity.ok(battleService.executeAction(battleId, action));
    }
}
