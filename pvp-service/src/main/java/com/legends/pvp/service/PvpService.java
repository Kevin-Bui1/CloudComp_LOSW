package com.legends.pvp.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PvpService handles the invitation flow for player-vs-player matches.
 *
 * For Deliverable 1, this is a stub implementation — the core invitation
 * mechanism works but it doesn't yet integrate with the Data Service (to verify
 * both players have saved parties) or the Battle Service (to actually run the match).
 * That integration is planned for Deliverable 2.
 *
 * How the invite flow works:
 *   1. Player A calls /invite → gets back an inviteId
 *   2. Player A shares that inviteId with Player B (out of band for now)
 *   3. Player B calls /accept with the inviteId → status changes to ACCEPTED
 *   4. After the match, /result records who won
 *
 * AtomicInteger ensures invite IDs are unique even if two requests come in at
 * the same time (thread-safe increment without needing a lock).
 */
@Service
public class PvpService {

    private final AtomicInteger inviteCounter = new AtomicInteger(1);
    private final Map<Integer, String> invites = new ConcurrentHashMap<>();

    /**
     * Creates a new invite and returns its ID.
     * The invite starts as PENDING until the other player accepts.
     */
    public int createInvite(int fromUserId, String toUsername) {
        int inviteId = inviteCounter.getAndIncrement();
        invites.put(inviteId, "PENDING");
        return inviteId;
    }

    /**
     * Accepts an invite if it exists. Returns false if the inviteId is not found,
     * which triggers a 400 Bad Request from the controller.
     */
    public boolean acceptInvite(int inviteId, int toUserId) {
        if (!invites.containsKey(inviteId)) {
            return false;
        }
        invites.put(inviteId, "ACCEPTED");
        return true;
    }

    /**
     * Records the outcome of a completed PvP match.
     * In D2 this will update the league table in the database.
     */
    public Map<String, Object> recordResult(int winnerUserId, int loserUserId) {
        return Map.of(
                "status", "RECORDED",
                "winnerUserId", winnerUserId,
                "loserUserId", loserUserId
        );
    }
}
