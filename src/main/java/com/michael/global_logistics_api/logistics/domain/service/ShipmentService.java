package com.michael.global_logistics_api.logistics.domain.service;

import com.michael.global_logistics_api.logistics.domain.model.Shipment;
import com.michael.global_logistics_api.logistics.domain.model.ShipmentStatus;
import com.michael.global_logistics_api.logistics.domain.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    @Transactional
    public Shipment createShipment(Shipment shipment) {
        // Validação de segurança
        if(shipment.getInsuranceValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do seguro deve ser maior que zero");
        }
        // Geração automática do tracking (Se não vier preenchido)
        if (shipment.getTrackingNumber() == null || shipment.getTrackingNumber().isEmpty()) {
            String prefix = "GLA-" + LocalDate.now().getYear() + "-";
            String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            shipment.setTrackingNumber(prefix + uniqueId);
        }

        // Garante o status incial
        // (Isso já pode estar no @PrePersist, mas garantir aqui é boa prática)
        shipment.setStatus(ShipmentStatus.PENDING);
        
        return shipmentRepository.save(shipment);
    }    

}
