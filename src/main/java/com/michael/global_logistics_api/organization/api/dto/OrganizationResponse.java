package com.michael.global_logistics_api.organization.api.dto;

import com.michael.global_logistics_api.organization.domain.OrganizationStatus;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String countryCode,
        String taxIdentifier,
        OrganizationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
