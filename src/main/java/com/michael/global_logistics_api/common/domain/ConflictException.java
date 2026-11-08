package com.michael.global_logistics_api.common.domain;

public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
