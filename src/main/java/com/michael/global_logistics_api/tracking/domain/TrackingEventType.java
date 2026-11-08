package com.michael.global_logistics_api.tracking.domain;

public enum TrackingEventType {
    PICKED_UP,
    DEPARTED,
    IN_TRANSIT,
    ARRIVED_AT_FACILITY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CUSTOMS_CLEARED,
    CUSTOMS_HOLD,
    EXCEPTION,
    CANCELLED,
    OTHER
}
