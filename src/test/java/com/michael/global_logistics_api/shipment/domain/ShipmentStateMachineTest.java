package com.michael.global_logistics_api.shipment.domain;

import com.michael.global_logistics_api.common.domain.ForbiddenException;
import com.michael.global_logistics_api.common.domain.InvalidStateTransitionException;
import com.michael.global_logistics_api.identity.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentStateMachineTest {

    @Test
    void pendingTransitionsToInTransitAndSetsActualDeparture() {
        Shipment shipment = shipmentWithStatus(ShipmentStatus.PENDING);

        ShipmentStateMachine.transition(shipment, ShipmentStatus.IN_TRANSIT, UserRole.LOGISTICS_OPERATOR);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(shipment.getActualDeparture()).isNotNull();
    }

    @Test
    void pendingTransitionsToCancelledByManager() {
        Shipment shipment = shipmentWithStatus(ShipmentStatus.PENDING);

        ShipmentStateMachine.transition(shipment, ShipmentStatus.CANCELLED, UserRole.LOGISTICS_MANAGER);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void pendingCannotBeCancelledByOperator() {
        Shipment shipment = shipmentWithStatus(ShipmentStatus.PENDING);

        assertThatThrownBy(() -> ShipmentStateMachine.transition(
                shipment, ShipmentStatus.CANCELLED, UserRole.LOGISTICS_OPERATOR))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void inTransitTransitionsToDeliveredAndSetsActualArrival() {
        Shipment shipment = shipmentWithStatus(ShipmentStatus.IN_TRANSIT);

        ShipmentStateMachine.transition(shipment, ShipmentStatus.DELIVERED, UserRole.LOGISTICS_OPERATOR);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getActualArrival()).isNotNull();
    }

    @Test
    void inTransitCanBeCancelledByAdminOrManager() {
        Shipment byAdmin = shipmentWithStatus(ShipmentStatus.IN_TRANSIT);
        ShipmentStateMachine.transition(byAdmin, ShipmentStatus.CANCELLED, UserRole.SYSTEM_ADMIN);
        assertThat(byAdmin.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);

        Shipment byManager = shipmentWithStatus(ShipmentStatus.IN_TRANSIT);
        ShipmentStateMachine.transition(byManager, ShipmentStatus.CANCELLED, UserRole.LOGISTICS_MANAGER);
        assertThat(byManager.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void inTransitCannotBeCancelledByOperator() {
        Shipment shipment = shipmentWithStatus(ShipmentStatus.IN_TRANSIT);

        assertThatThrownBy(() -> ShipmentStateMachine.transition(
                shipment, ShipmentStatus.CANCELLED, UserRole.LOGISTICS_OPERATOR))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void doesNotOverwriteExistingActualTimestamps() {
        Instant departure = Instant.parse("2026-01-01T10:00:00Z");
        Shipment shipment = shipmentWithStatus(ShipmentStatus.PENDING);
        shipment.setActualDeparture(departure);

        ShipmentStateMachine.transition(shipment, ShipmentStatus.IN_TRANSIT, UserRole.SYSTEM_ADMIN);

        assertThat(shipment.getActualDeparture()).isEqualTo(departure);
    }

    @Test
    void rejectsInvalidTransitions() {
        assertInvalid(shipmentWithStatus(ShipmentStatus.PENDING), ShipmentStatus.DELIVERED);
        assertInvalid(shipmentWithStatus(ShipmentStatus.PENDING), ShipmentStatus.PENDING);
        assertInvalid(shipmentWithStatus(ShipmentStatus.IN_TRANSIT), ShipmentStatus.PENDING);
        assertInvalid(shipmentWithStatus(ShipmentStatus.IN_TRANSIT), ShipmentStatus.IN_TRANSIT);
        assertInvalid(shipmentWithStatus(ShipmentStatus.DELIVERED), ShipmentStatus.CANCELLED);
        assertInvalid(shipmentWithStatus(ShipmentStatus.CANCELLED), ShipmentStatus.IN_TRANSIT);
        assertInvalid(shipmentWithStatus(ShipmentStatus.CANCELLED), ShipmentStatus.DELIVERED);
    }

    private void assertInvalid(Shipment shipment, ShipmentStatus target) {
        assertThatThrownBy(() -> ShipmentStateMachine.transition(shipment, target, UserRole.SYSTEM_ADMIN))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    private Shipment shipmentWithStatus(ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setStatus(status);
        return shipment;
    }
}
