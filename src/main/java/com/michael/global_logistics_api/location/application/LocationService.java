package com.michael.global_logistics_api.location.application;

import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import com.michael.global_logistics_api.location.api.dto.LocationResponse;
import com.michael.global_logistics_api.location.infrastructure.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<LocationResponse> list() {
        return locationRepository.findAll().stream()
                .map(LocationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse get(UUID id) {
        return locationRepository.findById(id)
                .map(LocationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
    }
}
