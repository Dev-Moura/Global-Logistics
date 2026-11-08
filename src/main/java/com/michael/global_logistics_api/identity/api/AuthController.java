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
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me();
    }
}
