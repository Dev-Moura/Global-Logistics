package com.michael.global_logistics_api.shipment.api;

import com.michael.global_logistics_api.common.api.ApiPaths;
import com.michael.global_logistics_api.shipment.application.ShipmentService;
import com.michael.global_logistics_api.shipment.domain.Shipment;
import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Shipment create(@RequestBody Shipment shipment) {
        return shipmentService.createShipment(shipment);
    }

    @GetMapping
    public List<Shipment> lists() {
        return shipmentService.findAll();
    }

    @GetMapping("/{id}")
    public Shipment search(@PathVariable UUID id) {
        return shipmentService.findById(id);
    }

    @PatchMapping("/{id}/status")
    public Shipment updateStatus(@PathVariable UUID id, @RequestParam ShipmentStatus newStatus) {
        return shipmentService.updateStatus(id, newStatus);
    }
}
