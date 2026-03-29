package com.legends.pvp.dto;

public record AcceptInviteRequest(
        int inviteId,
        int toUserId
) {}