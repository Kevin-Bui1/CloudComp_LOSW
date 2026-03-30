package com.legends.pvp.controller;

import com.legends.pvp.Invite;
import com.legends.pvp.dto.AcceptInviteRequest;
import com.legends.pvp.dto.InviteRequest;
import com.legends.pvp.dto.ResultRequest;
import com.legends.pvp.service.PvpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * PvP REST API on port 5004.
 *
 * POST /api/pvp/invite              — validate both players and create an invite
 * POST /api/pvp/accept              — accept invite and choose parties
 * POST /api/pvp/battle/{inviteId}   — load parties + start battle via battle-service
 * POST /api/pvp/result              — record win/loss in data-service
 */
@RestController
@RequestMapping("/api/pvp")
public class PvpController {

    private final PvpService pvpService;

    public PvpController(PvpService pvpService) {
        this.pvpService = pvpService;
    }

    @PostMapping("/invite")
    public ResponseEntity<?> invite(@RequestBody InviteRequest request) {
        try {
            String inviteId = pvpService.createInvite(request.fromUsername(), request.toUsername());
            return ResponseEntity.ok(Map.of("inviteId", inviteId, "status", "PENDING"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(@RequestBody AcceptInviteRequest request) {
        try {
            Invite invite = pvpService.acceptInvite(
                    request.inviteId(), request.senderParty(), request.receiverParty());
            return ResponseEntity.ok(Map.of(
                    "status",         "ACCEPTED",
                    "senderParty",    invite.getSenderParty(),
                    "receiverParty",  invite.getReceiverParty()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/battle/{inviteId}")
    public ResponseEntity<?> startBattle(@PathVariable String inviteId) {
        try {
            return ResponseEntity.ok(pvpService.startBattle(inviteId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/result")
    public ResponseEntity<?> result(@RequestBody ResultRequest request) {
        return ResponseEntity.ok(
                pvpService.recordResult(request.winnerUsername(), request.loserUsername()));
    }
}
