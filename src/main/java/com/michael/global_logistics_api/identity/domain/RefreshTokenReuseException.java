package com.michael.global_logistics_api.identity.domain;

import com.michael.global_logistics_api.common.domain.UnauthorizedException;

public class RefreshTokenReuseException extends UnauthorizedException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }
}
