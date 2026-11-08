package com.michael.global_logistics_api.identity.api.dto;

import com.michael.global_logistics_api.identity.domain.UserRole;

import java.util.UUID;

public record MeResponse(
        UUID userId,
        UUID organizationId,
        String organizationName,
        String fullName,
        String email,
        UserRole role
) {
}
