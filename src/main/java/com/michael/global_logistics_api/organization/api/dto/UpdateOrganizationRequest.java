package com.michael.global_logistics_api.organization.api.dto;

import com.michael.global_logistics_api.organization.domain.OrganizationStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 255) String name,
        @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode,
        @Size(max = 64) String taxIdentifier,
        OrganizationStatus status
) {
}
