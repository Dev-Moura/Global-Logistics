package com.michael.global_logistics_api.logistics.domain.Controller;


import com.michael.global_logistics_api.logistics.domain.model.Shipment;
import com.michael.global_logistics_api.logistics.domain.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService ShipmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Shipment create(@RequestBody Shipment shipment) {
        return ShipmentService.createShipment(shipment);
    }
}
