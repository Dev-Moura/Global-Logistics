package com.michael.global_logistics_api.carrier.api.dto;

import com.michael.global_logistics_api.carrier.domain.Carrier;
import com.michael.global_logistics_api.carrier.domain.CarrierStatus;

import java.util.UUID;

public record CarrierResponse(
        UUID id,
        String name,
        String countryCode,
        String contactInformation,
        String integrationType,
        CarrierStatus status
) {

    public static CarrierResponse from(Carrier carrier) {
        return new CarrierResponse(
                carrier.getId(),
                carrier.getName(),
                carrier.getCountryCode(),
                carrier.getContactInformation(),
                carrier.getIntegrationType(),
                carrier.getStatus()
        );
    }
}
