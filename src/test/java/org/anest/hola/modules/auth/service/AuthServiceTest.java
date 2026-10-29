package org.anest.hola.modules.auth.service;

import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.modules.auth.dto.AuthResponse;
import org.anest.hola.modules.auth.dto.LoginRequest;
import org.anest.hola.modules.auth.dto.LogoutRequest;
import org.anest.hola.modules.auth.dto.RefreshTokenRequest;
import org.anest.hola.modules.auth.dto.RegisterRequest;
import org.anest.hola.modules.auth.entity.RefreshToken;
import org.anest.hola.modules.auth.repository.RefreshTokenRepository;
import org.anest.hola.modules.user.dto.UserAuthDto;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.Role;
import org.anest.hola.modules.user.entity.UserStatus;
import org.anest.hola.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthService authService;

    private UserResponse activeUserResponse;

    @BeforeEach
    void setUp() {
        activeUserResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .build();
    }

    @Test
    void register_ShouldCreateActiveUserViaUserServiceAndReturnTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .email("  Student@Hola.edu.vn ")
                .password("Password123!")
                .fullName(" Nguyen Van A ")
                .build();

        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
        when(userService.createUser("  Student@Hola.edu.vn ", "hashed-password", " Nguyen Van A "))
                .thenReturn(activeUserResponse);
        when(jwtTokenService.generateAccessToken(activeUserResponse)).thenReturn("access-token");
        when(jwtTokenService.generateRawRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtTokenService.hashRefreshToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("raw-refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("student@hola.edu.vn");
        assertThat(response.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getUser().getRoles()).containsExactly(Role.USER);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo("hashed-refresh-token");
        assertThat(tokenCaptor.getValue().isRevoked()).isFalse();
    }

    @Test
    void register_WhenEmailAlreadyExists_ShouldPropagateUserAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("student@hola.edu.vn")
                .password("Password123!")
                .fullName("Nguyen Van A")
                .build();

        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
        when(userService.createUser("student@hola.edu.vn", "hashed-password", "Nguyen Van A"))
                .thenThrow(new AppException(ErrorCode.USER_ALREADY_EXISTS));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
    }

    @Test
    void login_WhenEmailDoesNotExist_ShouldStillRunPasswordEncoderToPreventTimingAttack() {
        LoginRequest request = LoginRequest.builder()
                .email("unknown@hola.edu.vn")
                .password("SomePassword123!")
                .build();

        when(userService.findAuthByEmail("unknown@hola.edu.vn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(passwordEncoder).matches(eq("SomePassword123!"), any(String.class));
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("student@hola.edu.vn")
                .password("WrongPassword")
                .build();

        UserAuthDto authDto = UserAuthDto.builder()
                .passwordHash("hashed-password")
                .user(activeUserResponse)
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(authDto));
        when(passwordEncoder.matches("WrongPassword", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void refresh_ShouldAtomicallyRevokeOldTokenAndIssueNewTokens() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(10L)
                .userId(activeUserResponse.getId())
                .tokenHash("old-hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(jwtTokenService.hashRefreshToken("old-raw-token")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.revokeIfActive(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(userService.findById(activeUserResponse.getId())).thenReturn(Optional.of(activeUserResponse));
        when(jwtTokenService.generateAccessToken(activeUserResponse)).thenReturn("new-access-token");
        when(jwtTokenService.generateRawRefreshToken()).thenReturn("new-raw-token");
        when(jwtTokenService.hashRefreshToken("new-raw-token")).thenReturn("new-hash");
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = authService.refresh(new RefreshTokenRequest("old-raw-token"));

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-raw-token");
        verify(refreshTokenRepository).revokeIfActive(eq(10L), any(LocalDateTime.class));
    }

    @Test
    void refresh_WhenConcurrentRequestAlreadyRevokedToken_ShouldThrowInvalidRefreshToken() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(10L)
                .userId(activeUserResponse.getId())
                .tokenHash("old-hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(jwtTokenService.hashRefreshToken("old-raw-token")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.revokeIfActive(eq(10L), any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("old-raw-token")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void refresh_WhenTokenIsAlreadyExpired_ShouldFailFastWithoutExecutingUpdate() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(10L)
                .userId(activeUserResponse.getId())
                .tokenHash("expired-hash")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .revoked(false)
                .build();

        when(jwtTokenService.hashRefreshToken("expired-raw-token")).thenReturn("expired-hash");
        when(refreshTokenRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired-raw-token")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(refreshTokenRepository, never()).revokeIfActive(any(), any());
    }

    @Test
    void logout_ShouldAtomicallyRevokeRefreshTokenByHash() {
        when(jwtTokenService.hashRefreshToken("old-raw-token")).thenReturn("old-hash");

        authService.logout(new LogoutRequest("old-raw-token"));

        verify(refreshTokenRepository).revokeByTokenHash(eq("old-hash"), any(LocalDateTime.class));
    }

    @Test
    void purgeRevokedOrExpiredTokens_ShouldCallRepositoryDelete() {
        when(refreshTokenRepository.deleteRevokedOrExpired(any(LocalDateTime.class))).thenReturn(5);

        int deleted = authService.purgeRevokedOrExpiredTokens();

        assertThat(deleted).isEqualTo(5);
        verify(refreshTokenRepository).deleteRevokedOrExpired(any(LocalDateTime.class));
    }
}
