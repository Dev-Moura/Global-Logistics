package com.michael.global_logistics_api.identity.api.dto;

import java.util.UUID;

public record RegisterResponse(UUID userId, UUID organizationId) {
}
