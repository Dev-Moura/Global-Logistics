package com.michael.global_logistics_api.common.security;

import com.michael.global_logistics_api.identity.domain.UserRole;

import java.util.UUID;

public record CurrentUser(UUID userId, UUID organizationId, UserRole role, String email) {
}
