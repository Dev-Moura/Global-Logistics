package com.michael.global_logistics_api.logistics.domain.service;

import com.michael.global_logistics_api.logistics.domain.model.Shipment;
import com.michael.global_logistics_api.logistics.domain.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    @Transactional
    public Shipment createShipment(Shipment shipment) {
        // Regra de négocio: Gerar um tracking number único para cada envio
        if(shipment.getTrackingNumber() == null) {
            shipment.setTrackingNumber("TRK--" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        return shipmentRepository.save(shipment);
    }    



}
