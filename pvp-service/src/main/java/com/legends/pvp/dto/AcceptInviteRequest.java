package com.legends.pvp.dto;

/** Sent by the invited player to accept a pending invite and pick their party. */
public record AcceptInviteRequest(String inviteId, String senderParty, String receiverParty) {}
