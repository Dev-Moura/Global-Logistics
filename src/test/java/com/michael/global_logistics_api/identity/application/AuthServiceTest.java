package com.michael.global_logistics_api.identity.application;

import com.michael.global_logistics_api.common.domain.ConflictException;
import com.michael.global_logistics_api.common.security.CurrentUserProvider;
import com.michael.global_logistics_api.common.security.JwtTokenService;
import com.michael.global_logistics_api.identity.api.dto.RefreshTokenRequest;
import com.michael.global_logistics_api.identity.api.dto.RegisterRequest;
import com.michael.global_logistics_api.identity.domain.RefreshToken;
import com.michael.global_logistics_api.identity.domain.RefreshTokenReuseException;
import com.michael.global_logistics_api.identity.domain.User;
import com.michael.global_logistics_api.identity.infrastructure.RefreshTokenRepository;
import com.michael.global_logistics_api.identity.infrastructure.UserRepository;
import com.michael.global_logistics_api.organization.infrastructure.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Duplicate Logistics",
                "US",
                "TAX-1",
                "Duplicate Admin",
                "duplicate@example.com",
                "StrongPass123!"
        );
        when(userRepository.existsByEmailIgnoreCase("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(ConflictException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("Email is already registered");
                    assertThat(exception.getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                });

        verify(organizationRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshRejectsRevokedTokenAndRevokesTokenFamily() {
        User user = new User();
        user.setId(UUID.randomUUID());

        RefreshToken revokedToken = new RefreshToken();
        revokedToken.setUser(user);
        revokedToken.setTokenHash("hashed-token");
        revokedToken.setRevokedAt(Instant.now());

        when(jwtTokenService.hashRefreshToken("raw-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("raw-token")))
                .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenRepository).revokeAllByUserId(any(UUID.class), any(Instant.class));
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(jwtTokenService.hashRefreshToken("unknown-token")).thenReturn("unknown-hash");
        when(refreshTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("unknown-token")))
                .hasMessage("Invalid refresh token");
    }
}
