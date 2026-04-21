package com.michael.global_logistics_api.logistics.domain.service;

import com.michael.global_logistics_api.logistics.domain.model.Shipment;
import com.michael.global_logistics_api.logistics.domain.model.ShipmentStatus;
import com.michael.global_logistics_api.logistics.domain.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    private BigDecimal calculateFreight(Double weight, Double distance, BigDecimal insurenceValue) {
        // componentes base (R$ 5,00/kg e R$ 0,50/km)
        double weightComponent = weight * 5.0;
        double distanceComponent = distance * 0.5;

        // Taxa de Risco (1% do valor do seguro)
        // se o seguro for nulo, vai ser tratado como zero assim no quebra o calculo
        BigDecimal riskTax = (insurenceValue != null)
                ? insurenceValue.multiply(new BigDecimal("0.01"))
                : BigDecimal.ZERO;

        // soma tudo e arredonda para 2 casas
        BigDecimal baseCost = BigDecimal.valueOf(weightComponent + distanceComponent);

        return baseCost.add(riskTax).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public Shipment createShipment(Shipment shipment) {
        // 1. Validação de peso
        if(shipment.getWeight() <= 0) {
            throw new RuntimeException("peso inválido para transporte internacional");
        }

        BigDecimal finalCost = calculateFreight(
                shipment.getWeight(),
                shipment.getDistance(),
                shipment.getInsuranceValue()
        );

        shipment.setShippingCost(finalCost);

        // 2. Regra de négocio: Gerar um tracking number único para cada envio
        if(shipment.getTrackingNumber() != null) {
            shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber())
                    .ifPresent(s -> {
                        throw new RuntimeException("Este Tracking Number já está em uso!");
                    });
        }   else {
            shipment.setTrackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        return shipmentRepository.save(shipment);
    }

    public Shipment updateStatus(UUID id, ShipmentStatus newStatus){

        // Busca a remessa ou retorna um 404
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Remessa nao encontrada"));

        // Validacao fluxo status -> no volta de DELIVERED para PENDING
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao e possivel alterar o status de uma carga ja entregue");
        }

        // Evita redundancia
        if (shipment.getStatus() == newStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A remessa ja possui o status: " + newStatus);
        }

        shipment.setStatus(newStatus);
        return shipmentRepository.save(shipment);
    }

}
