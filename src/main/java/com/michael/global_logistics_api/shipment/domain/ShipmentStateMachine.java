package com.michael.global_logistics_api.shipment.domain;

import com.michael.global_logistics_api.common.domain.ForbiddenException;
import com.michael.global_logistics_api.common.domain.InvalidStateTransitionException;
import com.michael.global_logistics_api.identity.domain.UserRole;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ShipmentStateMachine {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    private ShipmentStateMachine() {
    }

    public static void transition(Shipment shipment, ShipmentStatus target, UserRole role) {
        ShipmentStatus current = shipment.getStatus();

        if (current == null) {
            throw new InvalidStateTransitionException("Shipment has no current status");
        }

        Set<ShipmentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (target == null || !allowed.contains(target)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition shipment from " + current + " to " + target);
        }

        if (target == ShipmentStatus.CANCELLED
                && role != UserRole.SYSTEM_ADMIN && role != UserRole.LOGISTICS_MANAGER) {
            throw new ForbiddenException(
                    "Only SYSTEM_ADMIN or LOGISTICS_MANAGER can cancel a shipment");
        }

        if (target == ShipmentStatus.IN_TRANSIT && shipment.getActualDeparture() == null) {
            shipment.setActualDeparture(Instant.now());
        }

        if (target == ShipmentStatus.DELIVERED && shipment.getActualArrival() == null) {
            shipment.setActualArrival(Instant.now());
        }

        shipment.setStatus(target);
    }

    private static Map<ShipmentStatus, Set<ShipmentStatus>> buildTransitions() {
        Map<ShipmentStatus, Set<ShipmentStatus>> transitions = new EnumMap<>(ShipmentStatus.class);
        transitions.put(ShipmentStatus.PENDING,
                EnumSet.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.CANCELLED));
        transitions.put(ShipmentStatus.IN_TRANSIT,
                EnumSet.of(ShipmentStatus.DELIVERED, ShipmentStatus.CANCELLED));
        transitions.put(ShipmentStatus.DELIVERED, EnumSet.noneOf(ShipmentStatus.class));
        transitions.put(ShipmentStatus.CANCELLED, EnumSet.noneOf(ShipmentStatus.class));
        return transitions;
    }
}
