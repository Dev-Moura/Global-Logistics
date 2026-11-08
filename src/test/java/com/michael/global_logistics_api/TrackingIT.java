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
class TrackingIT extends AbstractIntegrationTest {

    private static final UUID SHANGHAI = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SANTOS = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void trackingLifecycleAppliesStatusesAndKeepsChronologicalHistory() throws Exception {
        String token = registerAndLogin("tracking-lifecycle");

        MvcResult created = mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createShipmentPayload()))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        String shipmentId = createdBody.get("id").asText();
        String trackingNumber = createdBody.get("trackingNumber").asText();

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("PICKED_UP", "2026-01-01T10:00:00Z", SHANGHAI, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipmentId").value(shipmentId))
                .andExpect(jsonPath("$.eventType").value("PICKED_UP"))
                .andExpect(jsonPath("$.location.id").value(SHANGHAI.toString()))
                .andExpect(jsonPath("$.eventTimestamp").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/shipments/" + shipmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.actualDeparture").isNotEmpty());

        MvcResult inTransit = mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("IN_TRANSIT", "2026-01-01T09:00:00Z", null, "EXT-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalEventId").value("EXT-1"))
                .andReturn();

        String inTransitEventId = objectMapper.readTree(inTransit.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("IN_TRANSIT", "2026-01-01T09:00:00Z", null, "EXT-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inTransitEventId));

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("DELIVERED", "2026-01-01T12:00:00Z", SANTOS, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("DELIVERED"));

        mockMvc.perform(get("/api/v1/shipments/" + shipmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.actualArrival").isNotEmpty());

        mockMvc.perform(get("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].eventType").value("IN_TRANSIT"))
                .andExpect(jsonPath("$[0].eventTimestamp").value("2026-01-01T09:00:00Z"))
                .andExpect(jsonPath("$[1].eventType").value("PICKED_UP"))
                .andExpect(jsonPath("$[1].eventTimestamp").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$[2].eventType").value("DELIVERED"))
                .andExpect(jsonPath("$[2].eventTimestamp").value("2026-01-01T12:00:00Z"));

        mockMvc.perform(get("/api/v1/tracking/" + trackingNumber)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value(trackingNumber))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.origin.id").value(SHANGHAI.toString()))
                .andExpect(jsonPath("$.destination.id").value(SANTOS.toString()))
                .andExpect(jsonPath("$.events.length()").value(3))
                .andExpect(jsonPath("$.events[0].eventType").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.events[2].eventType").value("DELIVERED"));

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventPayload("EXCEPTION", "2026-01-01T13:00:00Z", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRACKING_STATUS_TRANSITION"));

        mockMvc.perform(get("/api/v1/shipments/" + shipmentId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    private String createShipmentPayload() {
        return """
                {
                  "originId": "%s",
                  "destinationId": "%s",
                  "weightKg": 10,
                  "distanceKm": 100,
                  "insuredValue": 1000,
                  "estimatedDeparture": "2026-01-01T08:00:00Z",
                  "estimatedArrival": "2026-01-02T08:00:00Z"
                }
                """.formatted(SHANGHAI, SANTOS);
    }

    private String eventPayload(String eventType, String eventTimestamp, UUID locationId, String externalEventId) {
        String locationField = locationId == null ? "" : "\"locationId\":\"" + locationId + "\",";
        String externalField = externalEventId == null ? "" : "\"externalEventId\":\"" + externalEventId + "\",";

        return """
                {
                  "eventType": "%s",
                  %s
                  %s
                  "eventTimestamp": "%s"
                }
                """.formatted(eventType, locationField, externalField, eventTimestamp);
    }

    private String registerAndLogin(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "Tracking Logistics",
                                  "countryCode": "US",
                                  "taxIdentifier": "TAX-%s",
                                  "fullName": "Tracking Admin",
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
