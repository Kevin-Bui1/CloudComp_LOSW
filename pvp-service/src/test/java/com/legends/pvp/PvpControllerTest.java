package com.legends.pvp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legends.pvp.dto.AcceptInviteRequest;
import com.legends.pvp.dto.InviteRequest;
import com.legends.pvp.dto.ResultRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the PvP Service endpoints.
 *
 * I use MockMvc here instead of calling PvpService directly because I want to
 * test the full HTTP layer (routing, request parsing, response format) not just
 * the service logic. MockMvc lets me make fake HTTP requests without starting
 * a real server.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PvpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // PVP-TC-01: Health endpoint should return { "status": "ok" }
    @Test
    void healthShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/pvp/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // PVP-TC-02: Sending an invite should return PENDING status and a numeric inviteId
    @Test
    void inviteShouldReturnPending() throws Exception {
        InviteRequest request = new InviteRequest(1, "player2");

        mockMvc.perform(post("/api/pvp/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.inviteId").isNumber());
    }

    // PVP-TC-03: Accepting an invite that exists should return ACCEPTED
    @Test
    void acceptValidInviteShouldReturnAccepted() throws Exception {
        // First create an invite to get a real inviteId
        InviteRequest inviteRequest = new InviteRequest(10, "player20");
        MvcResult inviteResult = mockMvc.perform(post("/api/pvp/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteRequest)))
                .andExpect(status().isOk())
                .andReturn();

        int inviteId = objectMapper.readTree(
                inviteResult.getResponse().getContentAsString()
        ).get("inviteId").asInt();

        // Now accept it using the real inviteId
        AcceptInviteRequest acceptRequest = new AcceptInviteRequest(inviteId, 20);
        mockMvc.perform(post("/api/pvp/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    // PVP-TC-04: Accepting an invite that doesn't exist should return 400 with an error message
    @Test
    void acceptNonExistentInviteShouldReturnBadRequest() throws Exception {
        AcceptInviteRequest request = new AcceptInviteRequest(99999, 5);

        mockMvc.perform(post("/api/pvp/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invite not found"));
    }

    // PVP-TC-05: Recording a result should return RECORDED with both player IDs
    @Test
    void recordResultShouldReturnRecordedWithBothPlayers() throws Exception {
        ResultRequest request = new ResultRequest(1, 2);

        mockMvc.perform(post("/api/pvp/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECORDED"))
                .andExpect(jsonPath("$.winnerUserId").value(1))
                .andExpect(jsonPath("$.loserUserId").value(2));
    }

    // PVP-TC-06: Two separate invites should always get different IDs (AtomicInteger check)
    @Test
    void multipleInvitesShouldHaveUniqueIds() throws Exception {
        InviteRequest req1 = new InviteRequest(1, "alpha");
        InviteRequest req2 = new InviteRequest(2, "beta");

        MvcResult r1 = mockMvc.perform(post("/api/pvp/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk()).andReturn();

        MvcResult r2 = mockMvc.perform(post("/api/pvp/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk()).andReturn();

        int id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("inviteId").asInt();
        int id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("inviteId").asInt();

        assert id1 != id2 : "Each invite should have a unique ID";
    }

    // PVP-TC-07: The result response should correctly identify which player is the winner
    @Test
    void recordResultShouldCorrectlyDistinguishWinnerAndLoser() throws Exception {
        ResultRequest request = new ResultRequest(42, 99);

        mockMvc.perform(post("/api/pvp/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerUserId").value(42))
                .andExpect(jsonPath("$.loserUserId").value(99));
    }
}
