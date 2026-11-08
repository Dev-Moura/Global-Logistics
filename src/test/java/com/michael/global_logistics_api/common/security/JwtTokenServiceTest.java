package com.michael.global_logistics_api.common.security;

import com.michael.global_logistics_api.identity.domain.User;
import com.michael.global_logistics_api.identity.domain.UserRole;
import com.michael.global_logistics_api.organization.domain.Organization;
import com.michael.global_logistics_api.organization.domain.OrganizationStatus;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final String SECRET = "unit-test-secret-change-me-please-32bytes-min";

    private JwtProperties properties;
    private JwtTokenService jwtTokenService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTtl(Duration.ofMinutes(15));
        properties.setRefreshTtl(Duration.ofDays(7));
        properties.setIssuer("global-logistics-test");
        properties.setAudience("global-logistics-test-client");

        SecretKey secretKey = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        jwtTokenService = new JwtTokenService(jwtEncoder, properties);

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
        jwtDecoder = decoder;
    }

    @Test
    void issuesAccessTokenWithExpectedClaims() {
        User user = buildUser();

        JwtTokenService.IssuedAccessToken issuedAccessToken = jwtTokenService.issueAccessToken(user);
        Jwt jwt = jwtDecoder.decode(issuedAccessToken.token());

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("org_id")).isEqualTo(user.getOrganization().getId().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo(UserRole.LOGISTICS_MANAGER.name());
        assertThat(jwt.getClaimAsString("email")).isEqualTo(user.getEmail());
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(properties.getIssuer());
        assertThat(jwt.getAudience()).contains(properties.getAudience());
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(issuedAccessToken.expiresInSeconds()).isEqualTo(properties.getAccessTtl().toSeconds());
    }

    @Test
    void generatesOpaqueRefreshTokenAndDeterministicHash() {
        String first = jwtTokenService.generateRefreshToken();
        String second = jwtTokenService.generateRefreshToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).doesNotContain("=");

        String hash = jwtTokenService.hashRefreshToken(first);

        assertThat(hash).hasSize(64);
        assertThat(hash).isEqualTo(jwtTokenService.hashRefreshToken(first));
        assertThat(hash).isNotEqualTo(first);
    }

    @Test
    void computesRefreshTokenExpiryFromConfiguredTtl() {
        assertThat(jwtTokenService.refreshTokenExpiresAt())
                .isAfter(java.time.Instant.now().plus(properties.getRefreshTtl()).minusSeconds(5));
    }

    private User buildUser() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName("Test Logistics");
        organization.setCountryCode("US");
        organization.setStatus(OrganizationStatus.ACTIVE);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setFullName("Test Manager");
        user.setEmail("manager@example.com");
        user.setPasswordHash("hash");
        user.setRole(UserRole.LOGISTICS_MANAGER);
        user.setActive(true);
        return user;
    }
}
