package com.michael.global_logistics_api.tracking.domain;

import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingStatusResolverTest {

    @Test
    void inTransitEventsResolveToInTransit() {
        assertResolves(TrackingEventType.PICKED_UP, ShipmentStatus.IN_TRANSIT);
        assertResolves(TrackingEventType.DEPARTED, ShipmentStatus.IN_TRANSIT);
        assertResolves(TrackingEventType.IN_TRANSIT, ShipmentStatus.IN_TRANSIT);
        assertResolves(TrackingEventType.ARRIVED_AT_FACILITY, ShipmentStatus.IN_TRANSIT);
        assertResolves(TrackingEventType.OUT_FOR_DELIVERY, ShipmentStatus.IN_TRANSIT);
        assertResolves(TrackingEventType.CUSTOMS_CLEARED, ShipmentStatus.IN_TRANSIT);
        assertResolves(TrackingEventType.CUSTOMS_HOLD, ShipmentStatus.IN_TRANSIT);
        assertResolves(TrackingEventType.EXCEPTION, ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void deliveredAndCancelledEventsResolveToTerminalStatuses() {
        assertResolves(TrackingEventType.DELIVERED, ShipmentStatus.DELIVERED);
        assertResolves(TrackingEventType.CANCELLED, ShipmentStatus.CANCELLED);
    }

    @Test
    void otherEventDoesNotResolveAStatus() {
        assertThat(TrackingStatusResolver.resolve(TrackingEventType.OTHER)).isEmpty();
        assertThat(TrackingStatusResolver.resolve(null)).isEmpty();
    }

    private void assertResolves(TrackingEventType eventType, ShipmentStatus expected) {
        Optional<ShipmentStatus> resolved = TrackingStatusResolver.resolve(eventType);
        assertThat(resolved).contains(expected);
    }
}
