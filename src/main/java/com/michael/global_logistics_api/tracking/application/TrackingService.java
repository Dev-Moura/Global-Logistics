package com.michael.global_logistics_api.tracking.application;

import com.michael.global_logistics_api.common.domain.ConflictException;
import com.michael.global_logistics_api.common.domain.InvalidStateTransitionException;
import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import com.michael.global_logistics_api.common.security.CurrentUser;
import com.michael.global_logistics_api.common.security.CurrentUserProvider;
import com.michael.global_logistics_api.location.domain.Location;
import com.michael.global_logistics_api.location.infrastructure.LocationRepository;
import com.michael.global_logistics_api.shipment.domain.Shipment;
import com.michael.global_logistics_api.shipment.domain.ShipmentStateMachine;
import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;
import com.michael.global_logistics_api.shipment.infrastructure.ShipmentRepository;
import com.michael.global_logistics_api.tracking.api.dto.CreateTrackingEventRequest;
import com.michael.global_logistics_api.tracking.api.dto.TrackingEventResponse;
import com.michael.global_logistics_api.tracking.api.dto.TrackingInfoResponse;
import com.michael.global_logistics_api.tracking.domain.TrackingEvent;
import com.michael.global_logistics_api.tracking.domain.TrackingSource;
import com.michael.global_logistics_api.tracking.domain.TrackingStatusResolver;
import com.michael.global_logistics_api.tracking.infrastructure.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final LocationRepository locationRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TrackingMapper trackingMapper;

    @Transactional
    public RegistrationResult register(UUID shipmentId, CreateTrackingEventRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Shipment shipment = findOwnedShipment(shipmentId, currentUser.organizationId());

        String externalEventId = normalize(request.externalEventId());
        if (externalEventId != null) {
            Optional<TrackingEvent> existing = trackingEventRepository
                    .findByShipmentIdAndExternalEventId(shipmentId, externalEventId);

            if (existing.isPresent()) {
                return new RegistrationResult(trackingMapper.toResponse(existing.get()), false);
            }
        }

        Location location = resolveLocation(request.locationId());
        applyStatusTransition(shipment, request, currentUser);

        TrackingEvent event = new TrackingEvent();
        event.setShipment(shipment);
        event.setEventType(request.eventType());
        event.setDescription(request.description());
        event.setLocation(location);
        event.setEventTimestamp(request.eventTimestamp() == null ? Instant.now() : request.eventTimestamp());
        event.setSource(request.source() == null ? TrackingSource.MANUAL : request.source());
        event.setExternalEventId(externalEventId);

        TrackingEvent saved = trackingEventRepository.save(event);

        return new RegistrationResult(trackingMapper.toResponse(saved), true);
    }

    @Transactional(readOnly = true)
    public List<TrackingEventResponse> listByShipment(UUID shipmentId) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        findOwnedShipment(shipmentId, currentUser.organizationId());

        return trackingEventRepository.findByShipmentIdOrderByEventTimestampAscCreatedAtAsc(shipmentId)
                .stream()
                .map(trackingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackingInfoResponse track(String trackingNumber) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        Shipment shipment = shipmentRepository
                .findByTrackingNumberAndOrganizationId(trackingNumber, currentUser.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        List<TrackingEvent> events = trackingEventRepository
                .findByShipmentIdOrderByEventTimestampAscCreatedAtAsc(shipment.getId());

        return trackingMapper.toTrackingInfo(shipment, events);
    }

    private void applyStatusTransition(Shipment shipment, CreateTrackingEventRequest request, CurrentUser currentUser) {
        Optional<ShipmentStatus> targetStatus = TrackingStatusResolver.resolve(request.eventType());

        if (targetStatus.isEmpty() || targetStatus.get() == shipment.getStatus()) {
            return;
        }

        try {
            ShipmentStateMachine.transition(shipment, targetStatus.get(), currentUser.role());
        } catch (InvalidStateTransitionException exception) {
            throw new ConflictException("INVALID_TRACKING_STATUS_TRANSITION", exception.getMessage());
        }

        shipmentRepository.save(shipment);
    }

    private Shipment findOwnedShipment(UUID shipmentId, UUID organizationId) {
        return shipmentRepository.findByIdAndOrganizationId(shipmentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
    }

    private Location resolveLocation(UUID locationId) {
        if (locationId == null) {
            return null;
        }

        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record RegistrationResult(TrackingEventResponse event, boolean created) {
    }
}
