package org.anest.hola.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.anest.hola.common.dto.ApiResponse;
import org.anest.hola.common.security.SecurityUtils;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile & management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile")
    public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        return ApiResponse.success(userService.getCurrentUserProfile(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (Admin only)")
    public ApiResponse<UserResponse> getUserById(
            @PathVariable @Positive(message = "{validation.id.positive}") Long id) {
        return ApiResponse.success(userService.getById(id));
    }
}
