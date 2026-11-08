package com.michael.global_logistics_api;

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
class TrackingTenantIsolationIT extends AbstractIntegrationTest {

    private static final UUID SHANGHAI = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SANTOS = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void tenantCannotAccessAnotherOrganizationsTrackingData() throws Exception {
        String tokenA = registerAndLogin("tenant-a-tracking");
        String tokenB = registerAndLogin("tenant-b-tracking");

        MvcResult created = mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createShipmentPayload()))
                .andExpect(status().isCreated())
                .andReturn();

        String shipmentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();
        String trackingNumber = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("trackingNumber").asText();

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "PICKED_UP",
                                  "eventTimestamp": "2026-01-01T10:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "IN_TRANSIT",
                                  "eventTimestamp": "2026-01-01T11:00:00Z"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/tracking/" + trackingNumber)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private String createShipmentPayload() {
        return """
                {
                  "originId": "%s",
                  "destinationId": "%s",
                  "weightKg": 10,
                  "distanceKm": 100,
                  "insuredValue": 1000
                }
                """.formatted(SHANGHAI, SANTOS);
    }

    private String registerAndLogin(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "Tenant Tracking Logistics",
                                  "countryCode": "US",
                                  "taxIdentifier": "TAX-%s",
                                  "fullName": "Tenant Tracking Admin",
                                  "email": "%s",
                                  "password": "StrongPass123!"
                                }
                                """.formatted(UUID.randomUUID(), email)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
