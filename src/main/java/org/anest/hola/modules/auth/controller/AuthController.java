package org.anest.hola.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.anest.hola.common.dto.ApiResponse;
import org.anest.hola.common.i18n.MessageKeys;
import org.anest.hola.common.i18n.MessageUtils;
import org.anest.hola.modules.auth.dto.AuthResponse;
import org.anest.hola.modules.auth.dto.LoginRequest;
import org.anest.hola.modules.auth.dto.LogoutRequest;
import org.anest.hola.modules.auth.dto.RefreshTokenRequest;
import org.anest.hola.modules.auth.dto.RegisterRequest;
import org.anest.hola.modules.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, Token Refresh & Logout APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_REGISTER_SUCCESS), authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_LOGIN_SUCCESS), authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new access token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_REFRESH_SUCCESS), authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token and logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_LOGOUT_SUCCESS), null);
    }
}
