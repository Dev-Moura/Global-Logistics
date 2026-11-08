package com.michael.global_logistics_api.organization.application;

import com.michael.global_logistics_api.common.domain.DomainException;
import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import com.michael.global_logistics_api.common.security.CurrentUser;
import com.michael.global_logistics_api.common.security.CurrentUserProvider;
import com.michael.global_logistics_api.identity.domain.User;
import com.michael.global_logistics_api.identity.infrastructure.UserRepository;
import com.michael.global_logistics_api.organization.api.dto.CreateOrganizationRequest;
import com.michael.global_logistics_api.organization.api.dto.OrganizationResponse;
import com.michael.global_logistics_api.organization.api.dto.UpdateOrganizationRequest;
import com.michael.global_logistics_api.organization.api.dto.UserResponse;
import com.michael.global_logistics_api.organization.domain.Organization;
import com.michael.global_logistics_api.organization.domain.OrganizationStatus;
import com.michael.global_logistics_api.organization.infrastructure.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               CurrentUserProvider currentUserProvider) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.name().trim());
        organization.setCountryCode(request.countryCode().toUpperCase(Locale.ROOT));
        organization.setTaxIdentifier(normalizeNullable(request.taxIdentifier()));
        organization.setStatus(OrganizationStatus.ACTIVE);

        return toResponse(organizationRepository.save(organization));
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrent() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        return toResponse(findOrganization(currentUser.organizationId()));
    }

    @Transactional
    public OrganizationResponse updateCurrent(UpdateOrganizationRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Organization organization = findOrganization(currentUser.organizationId());

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new DomainException("INVALID_ORGANIZATION_NAME", "Organization name must not be blank");
            }
            organization.setName(name);
        }

        if (request.countryCode() != null) {
            organization.setCountryCode(request.countryCode().toUpperCase(Locale.ROOT));
        }

        if (request.taxIdentifier() != null) {
            organization.setTaxIdentifier(normalizeNullable(request.taxIdentifier()));
        }

        if (request.status() != null) {
            organization.setStatus(request.status());
        }

        return toResponse(organizationRepository.save(organization));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        return userRepository.findAllByOrganizationId(currentUser.organizationId()).stream()
                .map(this::toUserResponse)
                .toList();
    }

    private Organization findOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCountryCode(),
                organization.getTaxIdentifier(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
