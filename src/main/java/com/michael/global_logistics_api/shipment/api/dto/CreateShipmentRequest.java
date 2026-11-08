package com.michael.global_logistics_api.shipment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateShipmentRequest(
        @NotNull UUID originId,
        @NotNull UUID destinationId,
        UUID carrierId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
        @NotNull @DecimalMin(value = "0.0") BigDecimal distanceKm,
        @DecimalMin(value = "0.0") BigDecimal insuredValue,
        Instant estimatedDeparture,
        Instant estimatedArrival,
        @Size(max = 255) String trackingNumber
) {
}
