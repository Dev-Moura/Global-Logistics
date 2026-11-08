package com.michael.global_logistics_api.shipment.infrastructure;

import com.michael.global_logistics_api.shipment.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID>, JpaSpecificationExecutor<Shipment> {

    Optional<Shipment> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndTrackingNumber(UUID organizationId, String trackingNumber);
}
