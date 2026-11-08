package com.michael.global_logistics_api.shipment.domain;

import com.michael.global_logistics_api.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreightCalculatorTest {

    @Test
    void calculatesFreightWithPreviousPhaseFormula() {
        FreightDetails freight = FreightCalculator.calculate(
                new BigDecimal("10"),
                new BigDecimal("100"),
                new BigDecimal("1000")
        );

        assertThat(freight.getWeightKg()).isEqualByComparingTo("10");
        assertThat(freight.getDistanceKm()).isEqualByComparingTo("100");
        assertThat(freight.getInsuredValue()).isEqualByComparingTo("1000");
        assertThat(freight.getShippingCost()).isEqualByComparingTo("110.00");
        assertThat(freight.getShippingCost().scale()).isEqualTo(2);
        assertThat(freight.getCurrency()).isEqualTo("BRL");
    }

    @Test
    void treatsNullInsuranceValueAsZero() {
        FreightDetails freight = FreightCalculator.calculate(
                new BigDecimal("10"),
                new BigDecimal("100"),
                null
        );

        assertThat(freight.getInsuredValue()).isEqualByComparingTo("0");
        assertThat(freight.getShippingCost()).isEqualByComparingTo("100.00");
    }

    @Test
    void acceptsZeroDistanceAndZeroInsuredValue() {
        FreightDetails freight = FreightCalculator.calculate(
                new BigDecimal("2"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        assertThat(freight.getShippingCost()).isEqualByComparingTo("10.00");
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        FreightDetails freight = FreightCalculator.calculate(
                new BigDecimal("3"),
                new BigDecimal("7"),
                new BigDecimal("123.45")
        );

        assertThat(freight.getShippingCost()).isEqualByComparingTo("19.73");
    }

    @Test
    void rejectsNonPositiveWeight() {
        assertThatThrownBy(() -> FreightCalculator.calculate(
                BigDecimal.ZERO, new BigDecimal("10"), BigDecimal.ZERO))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_WEIGHT"));

        assertThatThrownBy(() -> FreightCalculator.calculate(
                new BigDecimal("-1"), new BigDecimal("10"), BigDecimal.ZERO))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> FreightCalculator.calculate(
                null, new BigDecimal("10"), BigDecimal.ZERO))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsNegativeDistance() {
        assertThatThrownBy(() -> FreightCalculator.calculate(
                new BigDecimal("10"), new BigDecimal("-1"), BigDecimal.ZERO))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_DISTANCE"));

        assertThatThrownBy(() -> FreightCalculator.calculate(
                new BigDecimal("10"), null, BigDecimal.ZERO))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsNegativeInsuredValue() {
        assertThatThrownBy(() -> FreightCalculator.calculate(
                new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("-0.01")))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_INSURED_VALUE"));
    }
}
