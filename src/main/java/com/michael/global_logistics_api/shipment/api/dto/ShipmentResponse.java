package com.michael.global_logistics_api.shipment.api.dto;

import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID organizationId,
        String trackingNumber,
        LocationSummaryResponse origin,
        LocationSummaryResponse destination,
        CarrierSummaryResponse carrier,
        ShipmentStatus status,
        FreightResponse freight,
        Instant estimatedDeparture,
        Instant estimatedArrival,
        Instant actualDeparture,
        Instant actualArrival,
        Instant createdAt,
        Instant updatedAt
) {
}
