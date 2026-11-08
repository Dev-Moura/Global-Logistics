package com.michael.global_logistics_api.shipment.api;

import com.michael.global_logistics_api.common.api.ApiPaths;
import com.michael.global_logistics_api.common.api.PageResponse;
import com.michael.global_logistics_api.shipment.api.dto.CreateShipmentRequest;
import com.michael.global_logistics_api.shipment.api.dto.ShipmentResponse;
import com.michael.global_logistics_api.shipment.api.dto.UpdateShipmentRequest;
import com.michael.global_logistics_api.shipment.api.dto.UpdateShipmentStatusRequest;
import com.michael.global_logistics_api.shipment.application.ShipmentFilter;
import com.michael.global_logistics_api.shipment.application.ShipmentService;
import com.michael.global_logistics_api.shipment.domain.ShipmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Gestão do ciclo de vida das remessas")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOGISTICS_MANAGER', 'LOGISTICS_OPERATOR')")
    @Operation(summary = "Criar remessa")
    public ShipmentResponse create(@Valid @RequestBody CreateShipmentRequest request) {
        return shipmentService.create(request);
    }

    @GetMapping
    @Operation(summary = "Listar remessas com filtros e paginação")
    public PageResponse<ShipmentResponse> list(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) UUID originId,
            @RequestParam(required = false) UUID destinationId,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ShipmentFilter filter = new ShipmentFilter(status, originId, destinationId, trackingNumber, from, to);
        return shipmentService.list(filter, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter remessa por identificador")
    public ShipmentResponse get(@PathVariable UUID id) {
        return shipmentService.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOGISTICS_MANAGER', 'LOGISTICS_OPERATOR')")
    @Operation(summary = "Atualizar dados da remessa")
    public ShipmentResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateShipmentRequest request) {
        return shipmentService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOGISTICS_MANAGER', 'LOGISTICS_OPERATOR')")
    @Operation(summary = "Atualizar status da remessa")
    public ShipmentResponse updateStatus(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateShipmentStatusRequest request) {
        return shipmentService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOGISTICS_MANAGER')")
    @Operation(summary = "Cancelar remessa")
    public ShipmentResponse cancel(@PathVariable UUID id) {
        return shipmentService.cancel(id);
    }
}
