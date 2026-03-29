package com.legends.pvp.dto;

public record ResultRequest(
        int winnerUserId,
        int loserUserId
) {}