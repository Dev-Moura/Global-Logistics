package com.michael.global_logistics_api.shipment.api.dto;

import com.michael.global_logistics_api.carrier.domain.CarrierStatus;

import java.util.UUID;

public record CarrierSummaryResponse(
        UUID id,
        String name,
        String countryCode,
        String integrationType,
        CarrierStatus status
) {
}
