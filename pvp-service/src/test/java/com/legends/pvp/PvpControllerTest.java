package com.legends.pvp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legends.pvp.dto.AcceptInviteRequest;
import com.legends.pvp.dto.InviteRequest;
import com.legends.pvp.dto.ResultRequest;
import com.legends.pvp.service.PvpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the PvP Service HTTP layer.
 *
 * PvpService makes real HTTP calls to data-service and battle-service,
 * so we mock it here with @MockBean to keep tests fast and isolated.
 * MockMvc tests the full HTTP routing, request parsing, and response format.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PvpControllerTest {

    @Autowired  private MockMvc       mockMvc;
    @Autowired  private ObjectMapper  objectMapper;
    @MockBean   private PvpService    pvpService;

    // PVP-TC-01: Health endpoint should return { "status": "ok" }
    @Test
    void healthShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/pvp/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // PVP-TC-02: Valid invite should return PENDING + an inviteId
    @Test
    void inviteShouldReturnPending() throws Exception {
        when(pvpService.createInvite("alice", "bob")).thenReturn("42");

        mockMvc.perform(post("/api/pvp/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteRequest("alice", "bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.inviteId").value("42"));
    }

    // PVP-TC-03: Invite to self should return 400
    @Test
    void inviteToSelf_shouldReturnBadRequest() throws Exception {
        when(pvpService.createInvite("alice", "alice"))
                .thenThrow(new IllegalArgumentException("You cannot invite yourself."));

        mockMvc.perform(post("/api/pvp/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteRequest("alice", "alice"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You cannot invite yourself."));
    }

    // PVP-TC-04: Invite to non-existent player should return 400
    @Test
    void inviteToNonExistentPlayer_shouldReturnBadRequest() throws Exception {
        when(pvpService.createInvite(any(), any()))
                .thenThrow(new IllegalArgumentException("Player 'ghost' not found or has no saved parties."));

        mockMvc.perform(post("/api/pvp/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InviteRequest("alice", "ghost"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // PVP-TC-05: Accepting a valid invite should return ACCEPTED
    @Test
    void acceptValidInvite_shouldReturnAccepted() throws Exception {
        com.legends.pvp.Invite invite = new com.legends.pvp.Invite("alice", "bob");
        invite.setSenderParty("Fellowship");
        invite.setReceiverParty("DarkLords");
        invite.accept();
        when(pvpService.acceptInvite("1", "Fellowship", "DarkLords")).thenReturn(invite);

        mockMvc.perform(post("/api/pvp/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AcceptInviteRequest("1", "Fellowship", "DarkLords"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    // PVP-TC-06: Accepting a non-existent invite should return 404
    @Test
    void acceptNonExistentInvite_shouldReturn404() throws Exception {
        when(pvpService.acceptInvite(any(), any(), any()))
                .thenThrow(new NoSuchElementException("Invite '99999' not found."));

        mockMvc.perform(post("/api/pvp/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AcceptInviteRequest("99999", "PartyA", "PartyB"))))
                .andExpect(status().isNotFound());
    }

    // PVP-TC-07: Recording a result should return RECORDED with both usernames
    @Test
    void recordResult_shouldReturnRecorded() throws Exception {
        when(pvpService.recordResult("alice", "bob"))
                .thenReturn(Map.of("status", "RECORDED",
                        "winnerUsername", "alice", "loserUsername", "bob"));

        mockMvc.perform(post("/api/pvp/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResultRequest("alice", "bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"))
                .andExpect(jsonPath("$.winnerUsername").value("alice"));
    }
}
