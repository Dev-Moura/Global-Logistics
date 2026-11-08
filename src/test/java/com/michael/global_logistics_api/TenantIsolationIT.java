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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TenantIsolationIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void usersOnlyListMembersOfTheirOwnOrganization() throws Exception {
        String tenantAEmail = uniqueEmail("tenant-a");
        String tenantBEmail = uniqueEmail("tenant-b");

        register(tenantAEmail, "Tenant A Logistics");
        register(tenantBEmail, "Tenant B Logistics");

        String tokenA = login(tenantAEmail);
        String tokenB = login(tenantBEmail);

        mockMvc.perform(get("/api/v1/organizations/users")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(tenantAEmail));

        mockMvc.perform(get("/api/v1/organizations/users")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(tenantBEmail));
    }

    @Test
    void currentOrganizationIsResolvedFromToken() throws Exception {
        String email = uniqueEmail("tenant-me");
        String organizationName = "Tenant Me Logistics";

        register(email, organizationName);
        String token = login(email);

        mockMvc.perform(get("/api/v1/organizations/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(organizationName))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private void register(String email, String organizationName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "%s",
                                  "countryCode": "US",
                                  "taxIdentifier": "TAX-%s",
                                  "fullName": "Tenant Admin",
                                  "email": "%s",
                                  "password": "StrongPass123!"
                                }
                                """.formatted(organizationName, UUID.randomUUID(), email)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
