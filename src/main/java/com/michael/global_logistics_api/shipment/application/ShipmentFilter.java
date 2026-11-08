package com.michael.global_logistics_api.shipment.application;

import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentFilter(
        ShipmentStatus status,
        UUID originId,
        UUID destinationId,
        String trackingNumber,
        Instant from,
        Instant to
) {
}
