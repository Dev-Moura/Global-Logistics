package com.michael.global_logistics_api.common.domain;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
