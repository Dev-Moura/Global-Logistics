package com.michael.global_logistics_api.shipment.domain;

import com.michael.global_logistics_api.common.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FreightCalculator {

    private static final BigDecimal WEIGHT_RATE = new BigDecimal("5");
    private static final BigDecimal DISTANCE_RATE = new BigDecimal("0.5");
    private static final BigDecimal INSURANCE_RATE = new BigDecimal("0.01");
    private static final int SCALE = 2;
    private static final String CURRENCY = "BRL";

    private FreightCalculator() {
    }

    public static FreightDetails calculate(BigDecimal weightKg, BigDecimal distanceKm, BigDecimal insuredValue) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("INVALID_WEIGHT", "peso inválido para transporte internacional");
        }

        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("INVALID_DISTANCE", "distância inválida para transporte internacional");
        }

        BigDecimal normalizedInsuredValue = insuredValue == null ? BigDecimal.ZERO : insuredValue;
        if (normalizedInsuredValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("INVALID_INSURED_VALUE", "valor segurado inválido para transporte internacional");
        }

        BigDecimal shippingCost = weightKg.multiply(WEIGHT_RATE)
                .add(distanceKm.multiply(DISTANCE_RATE))
                .add(normalizedInsuredValue.multiply(INSURANCE_RATE))
                .setScale(SCALE, RoundingMode.HALF_UP);

        return new FreightDetails(weightKg, distanceKm, normalizedInsuredValue, shippingCost, CURRENCY);
    }
}
