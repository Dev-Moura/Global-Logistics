package com.michael.global_logistics_api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michael.global_logistics_api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthFlowIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginMeRefreshAndLogout() throws Exception {
        String email = uniqueEmail("flow");
        register(email);

        JsonNode login = login(email);
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"));

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode refreshedTokens = objectMapper.readTree(refreshed.getResponse().getContentAsString());
        String newAccessToken = refreshedTokens.get("accessToken").asText();
        String newRefreshToken = refreshedTokens.get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void reusingRevokedRefreshTokenRevokesFamily() throws Exception {
        String email = uniqueEmail("reuse");
        register(email);

        JsonNode login = login(email);
        String originalRefreshToken = login.get("refreshToken").asText();

        MvcResult firstRefresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + originalRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String rotatedRefreshToken = objectMapper.readTree(firstRefresh.getResponse().getContentAsString())
                .get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + originalRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLoginWhenOrganizationIsInactive() throws Exception {
        String email = uniqueEmail("inactive");
        register(email);
        String accessToken = login(email).get("accessToken").asText();

        mockMvc.perform(patch("/api/v1/organizations/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {
        String email = uniqueEmail("invalid");
        register(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"WrongPass123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "Flow Logistics",
                                  "countryCode": "US",
                                  "taxIdentifier": "TAX-%s",
                                  "fullName": "Flow Admin",
                                  "email": "%s",
                                  "password": "StrongPass123!"
                                }
                                """.formatted(UUID.randomUUID(), email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.organizationId").isNotEmpty());
    }

    private JsonNode login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
