package com.michael.global_logistics_api.organization.api.dto;

import com.michael.global_logistics_api.identity.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role,
        boolean active,
        Instant createdAt
) {
}
