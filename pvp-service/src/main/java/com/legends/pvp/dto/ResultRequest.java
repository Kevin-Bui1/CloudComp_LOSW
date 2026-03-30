package com.legends.pvp.dto;

/** Posted after the battle completes to record the winner and loser usernames. */
public record ResultRequest(String winnerUsername, String loserUsername) {}
