package com.michael.global_logistics_api.common.domain;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
