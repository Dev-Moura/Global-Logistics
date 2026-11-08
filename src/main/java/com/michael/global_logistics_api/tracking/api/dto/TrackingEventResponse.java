package com.michael.global_logistics_api.tracking.api.dto;

import com.michael.global_logistics_api.shipment.api.dto.LocationSummaryResponse;
import com.michael.global_logistics_api.tracking.domain.TrackingEventType;
import com.michael.global_logistics_api.tracking.domain.TrackingSource;

import java.time.Instant;
import java.util.UUID;

public record TrackingEventResponse(
        UUID id,
        UUID shipmentId,
        TrackingEventType eventType,
        String description,
        LocationSummaryResponse location,
        Instant eventTimestamp,
        TrackingSource source,
        String externalEventId,
        Instant createdAt
) {
}
