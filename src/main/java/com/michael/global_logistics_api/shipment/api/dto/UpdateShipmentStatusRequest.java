package com.michael.global_logistics_api.shipment.api.dto;

import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateShipmentStatusRequest(@NotNull ShipmentStatus status) {
}
