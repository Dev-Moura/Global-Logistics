package com.michael.global_logistics_api.shipment.application;

import com.michael.global_logistics_api.carrier.domain.Carrier;
import com.michael.global_logistics_api.location.domain.Location;
import com.michael.global_logistics_api.shipment.api.dto.CarrierSummaryResponse;
import com.michael.global_logistics_api.shipment.api.dto.FreightResponse;
import com.michael.global_logistics_api.shipment.api.dto.LocationSummaryResponse;
import com.michael.global_logistics_api.shipment.api.dto.ShipmentResponse;
import com.michael.global_logistics_api.shipment.domain.FreightDetails;
import com.michael.global_logistics_api.shipment.domain.Shipment;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {

    public ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrganizationId(),
                shipment.getTrackingNumber(),
                toLocationSummary(shipment.getOrigin()),
                toLocationSummary(shipment.getDestination()),
                toCarrierSummary(shipment.getCarrier()),
                shipment.getStatus(),
                toFreightResponse(shipment.getFreight()),
                shipment.getEstimatedDeparture(),
                shipment.getEstimatedArrival(),
                shipment.getActualDeparture(),
                shipment.getActualArrival(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }

    public LocationSummaryResponse toLocationSummary(Location location) {
        if (location == null) {
            return null;
        }

        return new LocationSummaryResponse(
                location.getId(),
                location.getName(),
                location.getCity(),
                location.getState(),
                location.getCountryCode()
        );
    }

    public CarrierSummaryResponse toCarrierSummary(Carrier carrier) {
        if (carrier == null) {
            return null;
        }

        return new CarrierSummaryResponse(
                carrier.getId(),
                carrier.getName(),
                carrier.getCountryCode(),
                carrier.getIntegrationType(),
                carrier.getStatus()
        );
    }

    private FreightResponse toFreightResponse(FreightDetails freight) {
        if (freight == null) {
            return null;
        }

        return new FreightResponse(
                freight.getWeightKg(),
                freight.getDistanceKm(),
                freight.getInsuredValue(),
                freight.getShippingCost(),
                freight.getCurrency()
        );
    }
}
