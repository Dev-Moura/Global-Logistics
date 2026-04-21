package com.michael.global_logistics_api.logistics.domain.controller;


import com.michael.global_logistics_api.logistics.domain.model.Shipment;
import com.michael.global_logistics_api.logistics.domain.model.ShipmentStatus;
import com.michael.global_logistics_api.logistics.domain.repository.ShipmentRepository;
import com.michael.global_logistics_api.logistics.domain.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;

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
    public List<Shipment> lists() {
        return shipmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shipment> search(@PathVariable UUID id) {
        return shipmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public Shipment updateStatus(@PathVariable UUID id, @RequestParam ShipmentStatus newStatus) {
        return shipmentService.updateStatus(id, newStatus);
    }

}
