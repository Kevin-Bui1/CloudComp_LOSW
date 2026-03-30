package com.legends.pvp.dto;

/** Sent by the challenger to create a new PvP invite. */
public record InviteRequest(String fromUsername, String toUsername) {}
