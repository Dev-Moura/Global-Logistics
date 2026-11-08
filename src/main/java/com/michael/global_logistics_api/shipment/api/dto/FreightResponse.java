package com.michael.global_logistics_api.shipment.api.dto;

import java.math.BigDecimal;

public record FreightResponse(
        BigDecimal weightKg,
        BigDecimal distanceKm,
        BigDecimal insuredValue,
        BigDecimal shippingCost,
        String currency
) {
}
