package com.michael.global_logistics_api.identity.api.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
