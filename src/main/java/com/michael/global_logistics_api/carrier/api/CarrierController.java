package com.michael.global_logistics_api.carrier.api;

import com.michael.global_logistics_api.carrier.api.dto.CarrierResponse;
import com.michael.global_logistics_api.carrier.application.CarrierService;
import com.michael.global_logistics_api.common.api.ApiPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final CarrierService carrierService;

    @GetMapping
    public List<CarrierResponse> list() {
        return carrierService.list();
    }

    @GetMapping("/{id}")
    public CarrierResponse get(@PathVariable UUID id) {
        return carrierService.get(id);
    }
}
