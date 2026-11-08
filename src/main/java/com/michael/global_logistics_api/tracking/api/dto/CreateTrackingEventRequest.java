package com.michael.global_logistics_api.tracking.api.dto;

import com.michael.global_logistics_api.tracking.domain.TrackingEventType;
import com.michael.global_logistics_api.tracking.domain.TrackingSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateTrackingEventRequest(
        @NotNull TrackingEventType eventType,
        @Size(max = 1000) String description,
        UUID locationId,
        Instant eventTimestamp,
        TrackingSource source,
        @Size(max = 255) String externalEventId
) {
}
