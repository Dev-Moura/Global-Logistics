package com.michael.global_logistics_api.common.domain;

public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String message) {
        super("INVALID_STATE_TRANSITION", message);
    }
}
