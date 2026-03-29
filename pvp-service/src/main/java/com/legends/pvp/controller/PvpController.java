package com.legends.pvp.controller;

import com.legends.pvp.dto.AcceptInviteRequest;
import com.legends.pvp.dto.InviteRequest;
import com.legends.pvp.dto.ResultRequest;
import com.legends.pvp.service.PvpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pvp")
public class PvpController {

    private final PvpService pvpService;

    public PvpController(PvpService pvpService) {
        this.pvpService = pvpService;
    }

    @PostMapping("/invite")
    public ResponseEntity<Map<String, Object>> invite(@RequestBody InviteRequest request) {
        int inviteId = pvpService.createInvite(request.fromUserId(), request.toUsername());
        return ResponseEntity.ok(
                Map.of(
                        "inviteId", inviteId,
                        "status", "PENDING"
                )
        );
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(@RequestBody AcceptInviteRequest request) {
        boolean accepted = pvpService.acceptInvite(request.inviteId(), request.toUserId());
        if (!accepted) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invite not found")
            );
        }
        return ResponseEntity.ok(Map.of("status", "ACCEPTED"));
    }

    @PostMapping("/result")
    public ResponseEntity<Map<String, Object>> result(@RequestBody ResultRequest request) {
        return ResponseEntity.ok(
                pvpService.recordResult(request.winnerUserId(), request.loserUserId())
        );
    }
}