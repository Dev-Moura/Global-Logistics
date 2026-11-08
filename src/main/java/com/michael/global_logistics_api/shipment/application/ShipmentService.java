package com.michael.global_logistics_api.shipment.application;

import com.michael.global_logistics_api.carrier.domain.Carrier;
import com.michael.global_logistics_api.carrier.infrastructure.CarrierRepository;
import com.michael.global_logistics_api.common.api.PageResponse;
import com.michael.global_logistics_api.common.domain.ConflictException;
import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import com.michael.global_logistics_api.common.security.CurrentUser;
import com.michael.global_logistics_api.common.security.CurrentUserProvider;
import com.michael.global_logistics_api.location.domain.Location;
import com.michael.global_logistics_api.location.infrastructure.LocationRepository;
import com.michael.global_logistics_api.shipment.api.dto.CreateShipmentRequest;
import com.michael.global_logistics_api.shipment.api.dto.ShipmentResponse;
import com.michael.global_logistics_api.shipment.api.dto.UpdateShipmentRequest;
import com.michael.global_logistics_api.shipment.domain.FreightCalculator;
import com.michael.global_logistics_api.shipment.domain.FreightDetails;
import com.michael.global_logistics_api.shipment.domain.Shipment;
import com.michael.global_logistics_api.shipment.domain.ShipmentStateMachine;
import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;
import com.michael.global_logistics_api.shipment.domain.TrackingNumberGenerator;
import com.michael.global_logistics_api.shipment.infrastructure.ShipmentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_PROPERTY = "createdAt";

    private final ShipmentRepository shipmentRepository;
    private final LocationRepository locationRepository;
    private final CarrierRepository carrierRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ShipmentMapper shipmentMapper;

    @Transactional
    public ShipmentResponse create(CreateShipmentRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        UUID organizationId = currentUser.organizationId();

        Location origin = findLocation(request.originId());
        Location destination = findLocation(request.destinationId());
        Carrier carrier = request.carrierId() == null ? null : findCarrier(request.carrierId());

        String trackingNumber = normalizeTrackingNumber(request.trackingNumber());
        if (trackingNumber == null) {
            trackingNumber = TrackingNumberGenerator.generate();
        }

        if (shipmentRepository.existsByOrganizationIdAndTrackingNumber(organizationId, trackingNumber)) {
            throw new ConflictException("DUPLICATE_TRACKING_NUMBER",
                    "Este Tracking Number já está em uso!");
        }

        FreightDetails freight = FreightCalculator.calculate(
                request.weightKg(),
                request.distanceKm(),
                request.insuredValue()
        );

        Shipment shipment = new Shipment();
        shipment.setOrganizationId(organizationId);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setOrigin(origin);
        shipment.setDestination(destination);
        shipment.setCarrier(carrier);
        shipment.setFreight(freight);
        shipment.setEstimatedDeparture(request.estimatedDeparture());
        shipment.setEstimatedArrival(request.estimatedArrival());
        shipment.setStatus(ShipmentStatus.PENDING);

        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse get(UUID id) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        return shipmentMapper.toResponse(findOwned(id, currentUser.organizationId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> list(ShipmentFilter filter, Pageable pageable) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Pageable safePageable = sanitizePageable(pageable);

        Page<Shipment> page = shipmentRepository.findAll(
                buildSpecification(currentUser.organizationId(), filter),
                safePageable
        );

        return PageResponse.from(page.map(shipmentMapper::toResponse));
    }

    @Transactional
    public ShipmentResponse update(UUID id, UpdateShipmentRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Shipment shipment = findOwned(id, currentUser.organizationId());

        if (isTerminal(shipment.getStatus())) {
            throw new ConflictException("SHIPMENT_TERMINAL",
                    "Não é possível atualizar uma remessa em estado terminal");
        }

        if (request.originId() != null) {
            shipment.setOrigin(findLocation(request.originId()));
        }

        if (request.destinationId() != null) {
            shipment.setDestination(findLocation(request.destinationId()));
        }

        if (request.carrierId() != null) {
            shipment.setCarrier(findCarrier(request.carrierId()));
        }

        if (request.estimatedDeparture() != null) {
            shipment.setEstimatedDeparture(request.estimatedDeparture());
        }

        if (request.estimatedArrival() != null) {
            shipment.setEstimatedArrival(request.estimatedArrival());
        }

        return shipmentMapper.toResponse(shipmentRepository.saveAndFlush(shipment));
    }

    @Transactional
    public ShipmentResponse updateStatus(UUID id, ShipmentStatus newStatus) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Shipment shipment = findOwned(id, currentUser.organizationId());

        ShipmentStateMachine.transition(shipment, newStatus, currentUser.role());

        return shipmentMapper.toResponse(shipmentRepository.saveAndFlush(shipment));
    }

    @Transactional
    public ShipmentResponse cancel(UUID id) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Shipment shipment = findOwned(id, currentUser.organizationId());

        ShipmentStateMachine.transition(shipment, ShipmentStatus.CANCELLED, currentUser.role());

        return shipmentMapper.toResponse(shipmentRepository.saveAndFlush(shipment));
    }

    private Shipment findOwned(UUID id, UUID organizationId) {
        return shipmentRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
    }

    private Location findLocation(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
    }

    private Carrier findCarrier(UUID id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found"));
    }

    private Specification<Shipment> buildSpecification(UUID organizationId, ShipmentFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("organizationId"), organizationId));

            if (filter != null) {
                if (filter.status() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
                }

                if (filter.originId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("origin").get("id"), filter.originId()));
                }

                if (filter.destinationId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("destination").get("id"), filter.destinationId()));
                }

                String trackingNumber = normalizeTrackingNumber(filter.trackingNumber());
                if (trackingNumber != null) {
                    predicates.add(criteriaBuilder.equal(root.get("trackingNumber"), trackingNumber));
                }

                if (filter.from() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
                }

                if (filter.to() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.to()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, DEFAULT_SORT_PROPERTY);

        return PageRequest.of(pageNumber, pageSize, sort);
    }

    private boolean isTerminal(ShipmentStatus status) {
        return status == ShipmentStatus.DELIVERED || status == ShipmentStatus.CANCELLED;
    }

    private String normalizeTrackingNumber(String trackingNumber) {
        if (trackingNumber == null) {
            return null;
        }

        String trimmed = trackingNumber.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
