package com.michael.global_logistics_api.identity.api;

import com.michael.global_logistics_api.common.api.ApiPaths;
import com.michael.global_logistics_api.identity.api.dto.LoginRequest;
import com.michael.global_logistics_api.identity.api.dto.LogoutRequest;
import com.michael.global_logistics_api.identity.api.dto.MeResponse;
import com.michael.global_logistics_api.identity.api.dto.RefreshTokenRequest;
import com.michael.global_logistics_api.identity.api.dto.RegisterRequest;
import com.michael.global_logistics_api.identity.api.dto.RegisterResponse;
import com.michael.global_logistics_api.identity.api.dto.TokenResponse;
import com.michael.global_logistics_api.identity.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/auth")
@Tag(name = "Authentication", description = "Registro, autenticação e gestão de tokens")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar organização e usuário administrador")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e emitir tokens")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar tokens de acesso e atualização")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revogar token de atualização")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Obter usuário autenticado")
    public MeResponse me() {
        return authService.me();
    }
}
