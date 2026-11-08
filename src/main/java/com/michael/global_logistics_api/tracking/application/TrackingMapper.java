package com.michael.global_logistics_api.tracking.application;

import com.michael.global_logistics_api.shipment.application.ShipmentMapper;
import com.michael.global_logistics_api.shipment.domain.Shipment;
import com.michael.global_logistics_api.tracking.api.dto.TrackingEventResponse;
import com.michael.global_logistics_api.tracking.api.dto.TrackingInfoResponse;
import com.michael.global_logistics_api.tracking.domain.TrackingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TrackingMapper {

    private final ShipmentMapper shipmentMapper;

    public TrackingEventResponse toResponse(TrackingEvent event) {
        return new TrackingEventResponse(
                event.getId(),
                event.getShipment().getId(),
                event.getEventType(),
                event.getDescription(),
                shipmentMapper.toLocationSummary(event.getLocation()),
                event.getEventTimestamp(),
                event.getSource(),
                event.getExternalEventId(),
                event.getCreatedAt()
        );
    }

    public TrackingInfoResponse toTrackingInfo(Shipment shipment, List<TrackingEvent> events) {
        return new TrackingInfoResponse(
                shipment.getTrackingNumber(),
                shipment.getStatus(),
                shipmentMapper.toLocationSummary(shipment.getOrigin()),
                shipmentMapper.toLocationSummary(shipment.getDestination()),
                shipmentMapper.toCarrierSummary(shipment.getCarrier()),
                shipment.getEstimatedDeparture(),
                shipment.getEstimatedArrival(),
                shipment.getActualDeparture(),
                shipment.getActualArrival(),
                events.stream().map(this::toResponse).toList()
        );
    }
}
