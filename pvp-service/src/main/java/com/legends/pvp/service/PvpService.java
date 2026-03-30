package com.legends.pvp.service;

import com.legends.pvp.Invite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PvpService — real D1 implementation.
 *
 * Invite flow:
 *   1. createInvite  — validates both players exist + have parties (data-service)
 *   2. acceptInvite  — receiver picks their party
 *   3. startBattle   — loads parties from data-service, sends to battle-service
 *   4. recordResult  — posts win/loss to data-service
 */
@Service
public class PvpService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${data.service.url:http://localhost:5003}")
    private String dataServiceUrl;

    @Value("${battle.service.url:http://localhost:5001}")
    private String battleServiceUrl;

    private final Map<String, Invite> pendingInvites = new ConcurrentHashMap<>();
    private final AtomicInteger       idCounter      = new AtomicInteger(1);

    // ── 1. Create invite ────────────────────────────────────────────────
    public String createInvite(String fromUsername, String toUsername) {
        if (fromUsername == null || fromUsername.isBlank())
            throw new IllegalArgumentException("Sender username is required.");
        if (toUsername == null || toUsername.isBlank())
            throw new IllegalArgumentException("Recipient username is required.");
        if (fromUsername.equalsIgnoreCase(toUsername))
            throw new IllegalArgumentException("You cannot invite yourself.");

        checkPlayerEligible(fromUsername);
        checkPlayerEligible(toUsername);

        String inviteId = String.valueOf(idCounter.getAndIncrement());
        pendingInvites.put(inviteId, new Invite(fromUsername, toUsername));
        return inviteId;
    }

    // ── 2. Accept invite ────────────────────────────────────────────────
    public Invite acceptInvite(String inviteId, String senderParty, String receiverParty) {
        Invite invite = getInviteOrThrow(inviteId);
        if (senderParty == null || senderParty.isBlank())
            throw new IllegalArgumentException("Sender must choose a party.");
        if (receiverParty == null || receiverParty.isBlank())
            throw new IllegalArgumentException("Receiver must choose a party.");
        invite.setSenderParty(senderParty);
        invite.setReceiverParty(receiverParty);
        invite.accept();
        return invite;
    }

    // ── 3. Start battle ─────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> startBattle(String inviteId) {
        Invite invite = getInviteOrThrow(inviteId);
        if (!invite.isAccepted())
            throw new IllegalStateException("Invite has not been accepted yet.");

        List<?> senderHeroes   = loadPartyHeroes(invite.getSenderUsername(),   invite.getSenderParty());
        List<?> receiverHeroes = loadPartyHeroes(invite.getReceiverUsername(), invite.getReceiverParty());

        if (senderHeroes.isEmpty())
            throw new IllegalStateException(invite.getSenderUsername() + "'s party has no heroes.");
        if (receiverHeroes.isEmpty())
            throw new IllegalStateException(invite.getReceiverUsername() + "'s party has no heroes.");

        String battleId = "pvp-" + inviteId;
        Map<String, Object> battleRequest = new HashMap<>();
        battleRequest.put("playerUnits", senderHeroes);
        battleRequest.put("enemyUnits",  receiverHeroes);
        battleRequest.put("playerLabel", invite.getSenderUsername());
        battleRequest.put("enemyLabel",  invite.getReceiverUsername());

        Map<String, Object> response = restTemplate.postForObject(
                battleServiceUrl + "/api/battle/" + battleId + "/start",
                battleRequest,
                Map.class
        );
        return response != null ? response : Map.of("error", "No response from battle-service.");
    }

    // ── 4. Record result ────────────────────────────────────────────────
    public Map<String, Object> recordResult(String winnerUsername, String loserUsername) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("winnerUsername", winnerUsername);
            body.put("loserUsername",  loserUsername);
            restTemplate.postForObject(dataServiceUrl + "/api/data/pvp/result", body, Void.class);
        } catch (Exception e) {
            System.err.println("Warning: could not record PvP result: " + e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status",         "RECORDED");
        result.put("winnerUsername", winnerUsername);
        result.put("loserUsername",  loserUsername);
        return result;
    }

    // ── Helpers ─────────────────────────────────────────────────────────
    private void checkPlayerEligible(String username) {
        try {
            Map<?, ?> response = restTemplate.getForObject(
                    dataServiceUrl + "/api/data/players/" + username + "/eligible",
                    Map.class
            );
            if (response == null || !Boolean.TRUE.equals(response.get("eligible")))
                throw new IllegalArgumentException("Player '" + username + "' not found or has no saved parties.");
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("Player '" + username + "' not found or has no saved parties.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<?> loadPartyHeroes(String username, String partyName) {
        try {
            List<?> heroes = restTemplate.getForObject(
                    dataServiceUrl + "/api/data/players/" + username + "/parties/" + partyName + "/heroes",
                    List.class
            );
            return heroes != null ? heroes : List.of();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load party '" + partyName + "' for '" + username + "': " + e.getMessage());
        }
    }

    private Invite getInviteOrThrow(String inviteId) {
        Invite invite = pendingInvites.get(inviteId);
        if (invite == null) throw new NoSuchElementException("Invite '" + inviteId + "' not found.");
        return invite;
    }
}
