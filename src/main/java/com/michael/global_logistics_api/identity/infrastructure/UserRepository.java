package com.michael.global_logistics_api.identity.infrastructure;

import com.michael.global_logistics_api.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrganizationId(UUID organizationId);

    Optional<User> findByIdAndOrganization_Id(UUID id, UUID organizationId);
}
