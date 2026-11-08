package com.michael.global_logistics_api.location.api.dto;

import com.michael.global_logistics_api.location.domain.Location;

import java.util.UUID;

public record LocationResponse(
        UUID id,
        String name,
        String city,
        String state,
        String countryCode,
        Double latitude,
        Double longitude
) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getCity(),
                location.getState(),
                location.getCountryCode(),
                location.getLatitude(),
                location.getLongitude()
        );
    }
}
