package com.michael.global_logistics_api.logistics.domain.Controller;

import com.michael.global_logistics_api.logistics.domain.model.Shipment;
import com.michael.global_logistics_api.logistics.domain.model.ShipmentStatus;
import com.michael.global_logistics_api.logistics.domain.service.ShipmentService;
import com.michael.global_logistics_api.logistics.domain.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;


@RestController
@RequestMapping("/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentRepository shipmentRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Shipment create(@RequestBody Shipment shipment) {
        return shipmentService.createShipment(shipment);
    }

    @GetMapping
    public List<Shipment> list() {
        return shipmentRepository.findAll();
    }

    @PatchMapping("/{id}/status")
    public Shipment updateStatus(@PathVariable UUID id, @RequestParam ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        shipment.setStatus(newStatus);
        return shipmentRepository.save(shipment);
    }

}
