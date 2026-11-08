package com.michael.global_logistics_api.organization.api;

import com.michael.global_logistics_api.common.api.ApiPaths;
import com.michael.global_logistics_api.organization.api.dto.CreateOrganizationRequest;
import com.michael.global_logistics_api.organization.api.dto.OrganizationResponse;
import com.michael.global_logistics_api.organization.api.dto.UpdateOrganizationRequest;
import com.michael.global_logistics_api.organization.api.dto.UserResponse;
import com.michael.global_logistics_api.organization.application.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.create(request);
    }

    @GetMapping("/me")
    public OrganizationResponse getCurrent() {
        return organizationService.getCurrent();
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOGISTICS_MANAGER')")
    public OrganizationResponse updateCurrent(@Valid @RequestBody UpdateOrganizationRequest request) {
        return organizationService.updateCurrent(request);
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return organizationService.listUsers();
    }
}
