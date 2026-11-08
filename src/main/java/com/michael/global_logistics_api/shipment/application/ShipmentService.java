package com.michael.global_logistics_api.shipment.application;

import com.michael.global_logistics_api.common.domain.DomainException;
import com.michael.global_logistics_api.common.domain.InvalidStateTransitionException;
import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import com.michael.global_logistics_api.shipment.domain.Shipment;
import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;
import com.michael.global_logistics_api.shipment.infrastructure.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    private BigDecimal calculateFreight(Double weight, Double distance, BigDecimal insurenceValue) {
        double weightComponent = weight * 5.0;
        double distanceComponent = distance * 0.5;

        BigDecimal riskTax = (insurenceValue != null)
                ? insurenceValue.multiply(new BigDecimal("0.01"))
                : BigDecimal.ZERO;

        BigDecimal baseCost = BigDecimal.valueOf(weightComponent + distanceComponent);

        return baseCost.add(riskTax).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public Shipment createShipment(Shipment shipment) {
        if (shipment.getWeight() <= 0) {
            throw new DomainException("INVALID_WEIGHT", "peso inválido para transporte internacional");
        }

        BigDecimal finalCost = calculateFreight(
                shipment.getWeight(),
                shipment.getDistance(),
                shipment.getInsuranceValue()
        );

        shipment.setShippingCost(finalCost);

        if (shipment.getTrackingNumber() != null) {
            shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber())
                    .ifPresent(s -> {
                        throw new DomainException("DUPLICATE_TRACKING_NUMBER", "Este Tracking Number já está em uso!");
                    });
        } else {
            shipment.setTrackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        return shipmentRepository.save(shipment);
    }

    @Transactional(readOnly = true)
    public List<Shipment> findAll() {
        return shipmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Shipment findById(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Remessa nao encontrada"));
    }

    @Transactional
    public Shipment updateStatus(UUID id, ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Remessa nao encontrada"));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidStateTransitionException("Nao e possivel alterar o status de uma carga ja entregue");
        }

        if (shipment.getStatus() == newStatus) {
            throw new InvalidStateTransitionException("A remessa ja possui o status: " + newStatus);
        }

        shipment.setStatus(newStatus);
        return shipmentRepository.save(shipment);
    }
}
