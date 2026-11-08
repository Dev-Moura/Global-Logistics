package com.michael.global_logistics_api.tracking.infrastructure;

import com.michael.global_logistics_api.tracking.domain.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {

    Optional<TrackingEvent> findByShipmentIdAndExternalEventId(UUID shipmentId, String externalEventId);

    List<TrackingEvent> findByShipmentIdOrderByEventTimestampAscCreatedAtAsc(UUID shipmentId);

    boolean existsByShipmentIdAndExternalEventId(UUID shipmentId, String externalEventId);
}
