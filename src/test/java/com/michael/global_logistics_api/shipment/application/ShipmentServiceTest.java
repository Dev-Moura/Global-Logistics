package com.michael.global_logistics_api.shipment.application;

import com.michael.global_logistics_api.carrier.infrastructure.CarrierRepository;
import com.michael.global_logistics_api.common.domain.ConflictException;
import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import com.michael.global_logistics_api.common.security.CurrentUser;
import com.michael.global_logistics_api.common.security.CurrentUserProvider;
import com.michael.global_logistics_api.identity.domain.UserRole;
import com.michael.global_logistics_api.location.domain.Location;
import com.michael.global_logistics_api.location.infrastructure.LocationRepository;
import com.michael.global_logistics_api.shipment.api.dto.CreateShipmentRequest;
import com.michael.global_logistics_api.shipment.api.dto.ShipmentResponse;
import com.michael.global_logistics_api.shipment.domain.Shipment;
import com.michael.global_logistics_api.shipment.infrastructure.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CarrierRepository carrierRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentService(
                shipmentRepository,
                locationRepository,
                carrierRepository,
                currentUserProvider,
                new ShipmentMapper()
        );
    }

    @Test
    void createGeneratesTrackingNumberAndUsesOrganizationFromToken() {
        UUID organizationId = UUID.randomUUID();
        Location origin = location("Shanghai");
        Location destination = location("Santos");
        authenticate(organizationId, UserRole.LOGISTICS_OPERATOR);

        when(locationRepository.findById(origin.getId())).thenReturn(Optional.of(origin));
        when(locationRepository.findById(destination.getId())).thenReturn(Optional.of(destination));
        when(shipmentRepository.existsByOrganizationIdAndTrackingNumber(eq(organizationId), anyString()))
                .thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipment.setId(UUID.randomUUID());
            return shipment;
        });

        ShipmentResponse response = shipmentService.create(new CreateShipmentRequest(
                origin.getId(),
                destination.getId(),
                null,
                new BigDecimal("10"),
                new BigDecimal("100"),
                new BigDecimal("1000"),
                null,
                null,
                null
        ));

        assertThat(response.organizationId()).isEqualTo(organizationId);
        assertThat(response.trackingNumber()).matches("TRK-[A-Z0-9]{8}");
        assertThat(response.status().name()).isEqualTo("PENDING");
        assertThat(response.freight().shippingCost()).isEqualByComparingTo("110.00");
        assertThat(response.freight().currency()).isEqualTo("BRL");

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void createKeepsProvidedTrackingNumberWhenAvailable() {
        UUID organizationId = UUID.randomUUID();
        Location origin = location("Shanghai");
        Location destination = location("Santos");
        authenticate(organizationId, UserRole.SYSTEM_ADMIN);

        when(locationRepository.findById(origin.getId())).thenReturn(Optional.of(origin));
        when(locationRepository.findById(destination.getId())).thenReturn(Optional.of(destination));
        when(shipmentRepository.existsByOrganizationIdAndTrackingNumber(organizationId, "TRK-CUSTOM01"))
                .thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipment.setId(UUID.randomUUID());
            return shipment;
        });

        ShipmentResponse response = shipmentService.create(new CreateShipmentRequest(
                origin.getId(),
                destination.getId(),
                null,
                new BigDecimal("1"),
                new BigDecimal("1"),
                null,
                null,
                null,
                "TRK-CUSTOM01"
        ));

        assertThat(response.trackingNumber()).isEqualTo("TRK-CUSTOM01");
    }

    @Test
    void createRejectsDuplicateTrackingNumberWithinOrganization() {
        UUID organizationId = UUID.randomUUID();
        Location origin = location("Shanghai");
        Location destination = location("Santos");
        authenticate(organizationId, UserRole.SYSTEM_ADMIN);

        when(locationRepository.findById(origin.getId())).thenReturn(Optional.of(origin));
        when(locationRepository.findById(destination.getId())).thenReturn(Optional.of(destination));
        when(shipmentRepository.existsByOrganizationIdAndTrackingNumber(organizationId, "TRK-DUPLICATE"))
                .thenReturn(true);

        assertThatThrownBy(() -> shipmentService.create(new CreateShipmentRequest(
                origin.getId(),
                destination.getId(),
                null,
                new BigDecimal("1"),
                new BigDecimal("1"),
                null,
                null,
                null,
                "TRK-DUPLICATE"
        )))
                .isInstanceOfSatisfying(ConflictException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("DUPLICATE_TRACKING_NUMBER"));

        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void getHidesShipmentsFromAnotherOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        authenticate(organizationId, UserRole.SYSTEM_ADMIN);

        when(shipmentRepository.findByIdAndOrganizationId(shipmentId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.get(shipmentId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(shipmentRepository).findByIdAndOrganizationId(shipmentId, organizationId);
        verify(shipmentRepository, never()).findById(any());
    }

    private void authenticate(UUID organizationId, UserRole role) {
        when(currentUserProvider.getCurrentUser())
                .thenReturn(new CurrentUser(UUID.randomUUID(), organizationId, role, "user@example.com"));
    }

    private Location location(String city) {
        Location location = new Location();
        location.setId(UUID.randomUUID());
        location.setName(city + " Port");
        location.setCity(city);
        location.setCountryCode("BR");
        return location;
    }
}
