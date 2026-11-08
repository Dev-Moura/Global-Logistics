package com.michael.global_logistics_api.tracking.domain;

import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;

import java.util.Optional;

public final class TrackingStatusResolver {

    private TrackingStatusResolver() {
    }

    public static Optional<ShipmentStatus> resolve(TrackingEventType eventType) {
        if (eventType == null) {
            return Optional.empty();
        }

        return switch (eventType) {
            case PICKED_UP,
                    DEPARTED,
                    IN_TRANSIT,
                    ARRIVED_AT_FACILITY,
                    OUT_FOR_DELIVERY,
                    CUSTOMS_CLEARED,
                    CUSTOMS_HOLD,
                    EXCEPTION -> Optional.of(ShipmentStatus.IN_TRANSIT);
            case DELIVERED -> Optional.of(ShipmentStatus.DELIVERED);
            case CANCELLED -> Optional.of(ShipmentStatus.CANCELLED);
            case OTHER -> Optional.empty();
        };
    }
}
