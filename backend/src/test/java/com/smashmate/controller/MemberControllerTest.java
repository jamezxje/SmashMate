package com.smashmate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        var resp = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "admin@smashmate.local", "password", "Admin@123"))))
            .andReturn();
        adminToken = objectMapper.readTree(resp.getResponse().getContentAsString())
            .path("data").path("accessToken").asText();
    }

    @Test
    void createMember_asAdmin_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/members")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("fullName", "Test Member"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.fullName").value("Test Member"));
    }

    @Test
    void createGuest_asAdmin_roleIsGuest() throws Exception {
        mockMvc.perform(post("/api/v1/members/guests")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("fullName", "Khach VL"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.role").value("GUEST"));
    }

    @Test
    void createMember_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("fullName", "No Auth"))))
            .andExpect(status().isUnauthorized());
    }
}
