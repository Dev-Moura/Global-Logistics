package com.michael.global_logistics_api.shipment.api.dto;

import java.util.UUID;

public record LocationSummaryResponse(
        UUID id,
        String name,
        String city,
        String state,
        String countryCode
) {
}
