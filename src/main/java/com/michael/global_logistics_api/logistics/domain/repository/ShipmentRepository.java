package com.michael.global_logistics_api.logistics.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.michael.global_logistics_api.logistics.domain.model.Shipment;

import java.util.UUID;
import java.util.Optional;

@Repository // Esta anotação avisa ao spring que essa classe cuida do banco
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findBytrackingNumber(String trackingNumber);
}