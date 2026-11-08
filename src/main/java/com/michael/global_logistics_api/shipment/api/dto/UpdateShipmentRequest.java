package com.michael.global_logistics_api.shipment.api.dto;

import java.time.Instant;
import java.util.UUID;

public record UpdateShipmentRequest(
        UUID originId,
        UUID destinationId,
        UUID carrierId,
        Instant estimatedDeparture,
        Instant estimatedArrival
) {
}
