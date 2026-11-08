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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ShipmentTenantIsolationIT extends AbstractIntegrationTest {

    private static final UUID SHANGHAI = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SANTOS = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void tenantCannotAccessAnotherOrganizationsShipment() throws Exception {
        String tokenA = registerAndLogin("tenant-a-shipments");
        String tokenB = registerAndLogin("tenant-b-shipments");

        MvcResult created = mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isCreated())
                .andReturn();

        String shipmentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/api/v1/shipments/" + shipmentId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/shipments/" + shipmentId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/shipments")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(patch("/api/v1/shipments/" + shipmentId + "/status")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_TRANSIT\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/cancel")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private String createPayload() {
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
                                  "organizationName": "Tenant Shipment Logistics",
                                  "countryCode": "US",
                                  "taxIdentifier": "TAX-%s",
                                  "fullName": "Tenant Shipment Admin",
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
