package com.michael.global_logistics_api.shipment.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class FreightDetails {

    private BigDecimal weightKg;

    private BigDecimal distanceKm;

    private BigDecimal insuredValue;

    private BigDecimal shippingCost;

    private String currency;
}
