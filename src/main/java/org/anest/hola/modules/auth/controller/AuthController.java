package org.anest.hola.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.anest.hola.common.dto.ApiResponse;
import org.anest.hola.common.i18n.MessageKeys;
import org.anest.hola.common.i18n.MessageUtils;
import org.anest.hola.modules.auth.dto.AuthResponse;
import org.anest.hola.modules.auth.dto.ForgotPasswordRequest;
import org.anest.hola.modules.auth.dto.LoginRequest;
import org.anest.hola.modules.auth.dto.LogoutRequest;
import org.anest.hola.modules.auth.dto.RefreshTokenRequest;
import org.anest.hola.modules.auth.dto.RegisterRequest;
import org.anest.hola.modules.auth.dto.ResendOtpRequest;
import org.anest.hola.modules.auth.dto.ResetPasswordRequest;
import org.anest.hola.modules.auth.dto.VerifyEmailRequest;
import org.anest.hola.modules.auth.service.AuthService;
import org.anest.hola.modules.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Email Verification, Password Reset, Login & Token APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account and send verification OTP email")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_REGISTER_SUCCESS), authService.register(request)));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with 6-digit OTP and issue auth tokens")
    public ApiResponse<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_VERIFY_EMAIL_SUCCESS), authService.verifyEmail(request));
    }

    @PostMapping("/resend-verification-otp")
    @Operation(summary = "Resend email verification OTP code")
    public ApiResponse<Void> resendVerificationOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendVerificationOtp(request);
        return ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_RESEND_OTP_SUCCESS), null);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a 6-digit OTP to reset password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_FORGOT_PASSWORD_SUCCESS), null);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using 6-digit OTP")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(MessageUtils.get(MessageKeys.AUTH_RESET_PASSWORD_SUCCESS), null);
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
