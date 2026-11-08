package com.michael.global_logistics_api.carrier.infrastructure;

import com.michael.global_logistics_api.carrier.domain.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, UUID> {
}
