package com.michael.global_logistics_api.carrier.application;

import com.michael.global_logistics_api.carrier.api.dto.CarrierResponse;
import com.michael.global_logistics_api.carrier.infrastructure.CarrierRepository;
import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarrierService {

    private final CarrierRepository carrierRepository;

    @Transactional(readOnly = true)
    public List<CarrierResponse> list() {
        return carrierRepository.findAll().stream()
                .map(CarrierResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarrierResponse get(UUID id) {
        return carrierRepository.findById(id)
                .map(CarrierResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found"));
    }
}
