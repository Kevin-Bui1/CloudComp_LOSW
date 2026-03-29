package com.legends.pvp.dto;

public record InviteRequest(
        int fromUserId,
        String toUsername
) {}