package com.michael.global_logistics_api.location.infrastructure;

import com.michael.global_logistics_api.location.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
}
