package com.michael.global_logistics_api.tracking.api.dto;

import com.michael.global_logistics_api.shipment.api.dto.CarrierSummaryResponse;
import com.michael.global_logistics_api.shipment.api.dto.LocationSummaryResponse;
import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;

import java.time.Instant;
import java.util.List;

public record TrackingInfoResponse(
        String trackingNumber,
        ShipmentStatus status,
        LocationSummaryResponse origin,
        LocationSummaryResponse destination,
        CarrierSummaryResponse carrier,
        Instant estimatedDeparture,
        Instant estimatedArrival,
        Instant actualDeparture,
        Instant actualArrival,
        List<TrackingEventResponse> events
) {
}
