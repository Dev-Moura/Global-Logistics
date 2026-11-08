package com.michael.global_logistics_api.location.api;

import com.michael.global_logistics_api.common.api.ApiPaths;
import com.michael.global_logistics_api.location.api.dto.LocationResponse;
import com.michael.global_logistics_api.location.application.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationResponse> list() {
        return locationService.list();
    }

    @GetMapping("/{id}")
    public LocationResponse get(@PathVariable UUID id) {
        return locationService.get(id);
    }
}
