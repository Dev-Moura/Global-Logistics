package com.michael.global_logistics_api.tracking.application;

import com.michael.global_logistics_api.common.domain.ConflictException;
import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import com.michael.global_logistics_api.common.security.CurrentUser;
import com.michael.global_logistics_api.common.security.CurrentUserProvider;
import com.michael.global_logistics_api.identity.domain.UserRole;
import com.michael.global_logistics_api.location.infrastructure.LocationRepository;
import com.michael.global_logistics_api.shipment.application.ShipmentMapper;
import com.michael.global_logistics_api.shipment.domain.Shipment;
import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;
import com.michael.global_logistics_api.shipment.infrastructure.ShipmentRepository;
import com.michael.global_logistics_api.tracking.api.dto.CreateTrackingEventRequest;
import com.michael.global_logistics_api.tracking.domain.TrackingEvent;
import com.michael.global_logistics_api.tracking.domain.TrackingEventType;
import com.michael.global_logistics_api.tracking.domain.TrackingSource;
import com.michael.global_logistics_api.tracking.infrastructure.TrackingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private TrackingService trackingService;

    @BeforeEach
    void setUp() {
        trackingService = new TrackingService(
                trackingEventRepository,
                shipmentRepository,
                locationRepository,
                currentUserProvider,
                new TrackingMapper(new ShipmentMapper())
        );
    }

    @Test
    void returnsExistingEventWhenExternalEventIdWasAlreadyRegistered() {
        UUID organizationId = UUID.randomUUID();
        Shipment shipment = shipment(ShipmentStatus.PENDING);
        TrackingEvent existing = event(shipment, TrackingEventType.PICKED_UP);
        existing.setExternalEventId("EXT-1");
        authenticate(organizationId, UserRole.LOGISTICS_OPERATOR);

        when(shipmentRepository.findByIdAndOrganizationId(shipment.getId(), organizationId))
                .thenReturn(Optional.of(shipment));
        when(trackingEventRepository.findByShipmentIdAndExternalEventId(shipment.getId(), "EXT-1"))
                .thenReturn(Optional.of(existing));

        TrackingService.RegistrationResult result = trackingService.register(
                shipment.getId(),
                new CreateTrackingEventRequest(
                        TrackingEventType.PICKED_UP,
                        "duplicate",
                        null,
                        Instant.parse("2026-01-01T10:00:00Z"),
                        TrackingSource.EXTERNAL,
                        "EXT-1"
                )
        );

        assertThat(result.created()).isFalse();
        assertThat(result.event().id()).isEqualTo(existing.getId());
        verify(trackingEventRepository, never()).save(any(TrackingEvent.class));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void hidesEventsFromAnotherOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        authenticate(organizationId, UserRole.SYSTEM_ADMIN);

        when(shipmentRepository.findByIdAndOrganizationId(shipmentId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.listByShipment(shipmentId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(trackingEventRepository, never())
                .findByShipmentIdOrderByEventTimestampAscCreatedAtAsc(any());
    }

    @Test
    void rejectsInvalidStatusTransitionWithoutSavingEvent() {
        UUID organizationId = UUID.randomUUID();
        Shipment shipment = shipment(ShipmentStatus.PENDING);
        authenticate(organizationId, UserRole.SYSTEM_ADMIN);

        when(shipmentRepository.findByIdAndOrganizationId(shipment.getId(), organizationId))
                .thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> trackingService.register(
                shipment.getId(),
                new CreateTrackingEventRequest(
                        TrackingEventType.DELIVERED,
                        null,
                        null,
                        Instant.parse("2026-01-01T10:00:00Z"),
                        TrackingSource.MANUAL,
                        null
                )
        ))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_TRACKING_STATUS_TRANSITION"));

        verify(trackingEventRepository, never()).save(any(TrackingEvent.class));
    }

    @Test
    void appliesTransitionAndPreservesEventTimestamp() {
        UUID organizationId = UUID.randomUUID();
        Shipment shipment = shipment(ShipmentStatus.PENDING);
        Instant eventTimestamp = Instant.parse("2026-01-01T10:00:00Z");
        authenticate(organizationId, UserRole.LOGISTICS_OPERATOR);

        when(shipmentRepository.findByIdAndOrganizationId(shipment.getId(), organizationId))
                .thenReturn(Optional.of(shipment));
        when(trackingEventRepository.save(any(TrackingEvent.class))).thenAnswer(invocation -> {
            TrackingEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        TrackingService.RegistrationResult result = trackingService.register(
                shipment.getId(),
                new CreateTrackingEventRequest(
                        TrackingEventType.PICKED_UP,
                        "Picked up",
                        null,
                        eventTimestamp,
                        null,
                        null
                )
        );

        assertThat(result.created()).isTrue();
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(shipment.getActualDeparture()).isNotNull();

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(trackingEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventTimestamp()).isEqualTo(eventTimestamp);
        assertThat(captor.getValue().getSource()).isEqualTo(TrackingSource.MANUAL);
        assertThat(captor.getValue().getShipment()).isSameAs(shipment);
    }

    private void authenticate(UUID organizationId, UserRole role) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUser(UUID.randomUUID(), organizationId, role, "user@example.com"));
    }

    private Shipment shipment(ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setTrackingNumber("TRK-TEST0001");
        shipment.setStatus(status);
        return shipment;
    }

    private TrackingEvent event(Shipment shipment, TrackingEventType eventType) {
        TrackingEvent event = new TrackingEvent();
        event.setId(UUID.randomUUID());
        event.setShipment(shipment);
        event.setEventType(eventType);
        event.setEventTimestamp(Instant.parse("2026-01-01T10:00:00Z"));
        event.setSource(TrackingSource.EXTERNAL);
        return event;
    }
}
