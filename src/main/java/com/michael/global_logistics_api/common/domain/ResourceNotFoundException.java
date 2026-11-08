package com.michael.global_logistics_api.common.domain;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
