package com.michael.global_logistics_api.location.api;

import com.michael.global_logistics_api.common.api.ApiPaths;
import com.michael.global_logistics_api.location.api.dto.LocationResponse;
import com.michael.global_logistics_api.location.application.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Locations", description = "Consulta de locais de origem e destino")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @Operation(summary = "Listar locais")
    public List<LocationResponse> list() {
        return locationService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter local por identificador")
    public LocationResponse get(@PathVariable UUID id) {
        return locationService.get(id);
    }
}
