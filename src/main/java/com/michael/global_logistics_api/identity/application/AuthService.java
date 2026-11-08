package com.michael.global_logistics_api.identity.application;

import com.michael.global_logistics_api.common.domain.ConflictException;
import com.michael.global_logistics_api.common.domain.ResourceNotFoundException;
import com.michael.global_logistics_api.common.domain.UnauthorizedException;
import com.michael.global_logistics_api.common.security.CurrentUser;
import com.michael.global_logistics_api.common.security.CurrentUserProvider;
import com.michael.global_logistics_api.common.security.JwtTokenService;
import com.michael.global_logistics_api.identity.api.dto.LoginRequest;
import com.michael.global_logistics_api.identity.api.dto.MeResponse;
import com.michael.global_logistics_api.identity.api.dto.RefreshTokenRequest;
import com.michael.global_logistics_api.identity.api.dto.RegisterRequest;
import com.michael.global_logistics_api.identity.api.dto.RegisterResponse;
import com.michael.global_logistics_api.identity.api.dto.TokenResponse;
import com.michael.global_logistics_api.identity.domain.RefreshToken;
import com.michael.global_logistics_api.identity.domain.RefreshTokenReuseException;
import com.michael.global_logistics_api.identity.domain.User;
import com.michael.global_logistics_api.identity.domain.UserRole;
import com.michael.global_logistics_api.identity.infrastructure.RefreshTokenRepository;
import com.michael.global_logistics_api.identity.infrastructure.UserRepository;
import com.michael.global_logistics_api.organization.domain.Organization;
import com.michael.global_logistics_api.organization.domain.OrganizationStatus;
import com.michael.global_logistics_api.organization.infrastructure.OrganizationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserProvider currentUserProvider;

    public AuthService(OrganizationRepository organizationRepository,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       CurrentUserProvider currentUserProvider) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email is already registered");
        }

        Organization organization = new Organization();
        organization.setName(request.organizationName().trim());
        organization.setCountryCode(request.countryCode().toUpperCase(Locale.ROOT));
        organization.setTaxIdentifier(normalizeNullable(request.taxIdentifier()));
        organization.setStatus(OrganizationStatus.ACTIVE);
        Organization savedOrganization = organizationRepository.save(organization);

        User user = new User();
        user.setOrganization(savedOrganization);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.SYSTEM_ADMIN);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        return new RegisterResponse(savedUser.getId(), savedOrganization.getId());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()) || !user.isActive()) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return issueTokens(user);
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public TokenResponse refresh(RefreshTokenRequest request) {
        String tokenHash = jwtTokenService.hashRefreshToken(request.refreshToken());
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (currentToken.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllByUserId(currentToken.getUser().getId(), Instant.now());
            throw new RefreshTokenReuseException();
        }

        if (currentToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = currentToken.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String rawRefreshToken = jwtTokenService.generateRefreshToken();
        RefreshToken replacement = buildRefreshToken(user, rawRefreshToken);
        RefreshToken savedReplacement = refreshTokenRepository.save(replacement);

        currentToken.setRevokedAt(Instant.now());
        currentToken.setReplacedByTokenId(savedReplacement.getId());
        refreshTokenRepository.save(currentToken);

        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(user);
        return new TokenResponse(accessToken.token(), rawRefreshToken, "Bearer", accessToken.expiresInSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = jwtTokenService.hashRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        User user = userRepository.findByIdAndOrganization_Id(currentUser.userId(), currentUser.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new MeResponse(
                user.getId(),
                user.getOrganization().getId(),
                user.getOrganization().getName(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }

    private TokenResponse issueTokens(User user) {
        String rawRefreshToken = jwtTokenService.generateRefreshToken();
        refreshTokenRepository.save(buildRefreshToken(user, rawRefreshToken));

        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(user);
        return new TokenResponse(accessToken.token(), rawRefreshToken, "Bearer", accessToken.expiresInSeconds());
    }

    private RefreshToken buildRefreshToken(User user, String rawRefreshToken) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(jwtTokenService.hashRefreshToken(rawRefreshToken));
        refreshToken.setExpiresAt(jwtTokenService.refreshTokenExpiresAt());
        return refreshToken;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
