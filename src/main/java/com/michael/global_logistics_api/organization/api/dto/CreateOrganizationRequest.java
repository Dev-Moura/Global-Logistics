package com.michael.global_logistics_api.organization.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String countryCode,
        @Size(max = 64) String taxIdentifier
) {
}
