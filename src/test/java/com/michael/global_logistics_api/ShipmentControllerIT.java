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

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ShipmentControllerIT extends AbstractIntegrationTest {

    private static final UUID SHANGHAI = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SANTOS = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MAERSK = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shipmentLifecycle() throws Exception {
        String token = registerAndLogin("shipment-lifecycle");

        MvcResult created = mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.organizationId").isNotEmpty())
                .andExpect(jsonPath("$.trackingNumber", startsWith("TRK-")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.origin.id").value(SHANGHAI.toString()))
                .andExpect(jsonPath("$.destination.id").value(SANTOS.toString()))
                .andExpect(jsonPath("$.freight.weightKg").value(10))
                .andExpect(jsonPath("$.freight.distanceKm").value(100))
                .andExpect(jsonPath("$.freight.insuredValue").value(1000))
                .andExpect(jsonPath("$.freight.shippingCost").value(110.0))
                .andExpect(jsonPath("$.freight.currency").value("BRL"))
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        String shipmentId = createdBody.get("id").asText();
        String trackingNumber = createdBody.get("trackingNumber").asText();

        mockMvc.perform(get("/api/v1/shipments")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "PENDING")
                        .param("trackingNumber", trackingNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(shipmentId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/v1/shipments/" + shipmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value(trackingNumber));

        mockMvc.perform(patch("/api/v1/shipments/" + shipmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "%s",
                                  "estimatedArrival": "2026-12-01T10:00:00Z"
                                }
                                """.formatted(MAERSK)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carrier.id").value(MAERSK.toString()))
                .andExpect(jsonPath("$.estimatedArrival").value("2026-12-01T10:00:00Z"));

        mockMvc.perform(patch("/api/v1/shipments/" + shipmentId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_TRANSIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.actualDeparture").isNotEmpty());

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(patch("/api/v1/shipments/" + shipmentId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(patch("/api/v1/shipments/" + shipmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estimatedArrival\":\"2027-01-01T10:00:00Z\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHIPMENT_TERMINAL"));
    }

    @Test
    void rejectsDuplicateTrackingNumberWithinOrganization() throws Exception {
        String token = registerAndLogin("shipment-duplicate");

        mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("TRK-REPEAT01")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("TRK-REPEAT01")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TRACKING_NUMBER"));
    }

    @Test
    void exposesLocationAndCarrierCatalog() throws Exception {
        String token = registerAndLogin("shipment-catalog");

        mockMvc.perform(get("/api/v1/locations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.countryCode == 'CN')].id").isNotEmpty());

        mockMvc.perform(get("/api/v1/locations/" + SHANGHAI)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("CN"));

        mockMvc.perform(get("/api/v1/carriers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/v1/carriers/" + MAERSK)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private String createPayload(String trackingNumber) {
        String trackingField = trackingNumber == null
                ? ""
                : "\"trackingNumber\":\"" + trackingNumber + "\",";

        return """
                {
                  %s
                  "originId": "%s",
                  "destinationId": "%s",
                  "carrierId": "%s",
                  "weightKg": 10,
                  "distanceKm": 100,
                  "insuredValue": 1000,
                  "estimatedDeparture": "2026-11-10T08:00:00Z"
                }
                """.formatted(trackingField, SHANGHAI, SANTOS, MAERSK);
    }

    private String registerAndLogin(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "Shipment Logistics",
                                  "countryCode": "US",
                                  "taxIdentifier": "TAX-%s",
                                  "fullName": "Shipment Admin",
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
