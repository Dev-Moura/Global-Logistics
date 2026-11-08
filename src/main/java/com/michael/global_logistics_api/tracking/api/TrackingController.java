package com.michael.global_logistics_api.tracking.api;

import com.michael.global_logistics_api.common.api.ApiPaths;
import com.michael.global_logistics_api.tracking.api.dto.CreateTrackingEventRequest;
import com.michael.global_logistics_api.tracking.api.dto.TrackingEventResponse;
import com.michael.global_logistics_api.tracking.api.dto.TrackingInfoResponse;
import com.michael.global_logistics_api.tracking.application.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.API_V1)
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/shipments/{shipmentId}/events")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOGISTICS_MANAGER', 'LOGISTICS_OPERATOR')")
    public ResponseEntity<TrackingEventResponse> register(@PathVariable UUID shipmentId,
                                                          @Valid @RequestBody CreateTrackingEventRequest request) {
        TrackingService.RegistrationResult result = trackingService.register(shipmentId, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.event());
    }

    @GetMapping("/shipments/{shipmentId}/events")
    public List<TrackingEventResponse> listByShipment(@PathVariable UUID shipmentId) {
        return trackingService.listByShipment(shipmentId);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public TrackingInfoResponse track(@PathVariable String trackingNumber) {
        return trackingService.track(trackingNumber);
    }
}
