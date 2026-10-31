package org.anest.hola.modules.auth.service;

import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.modules.auth.dto.AuthResponse;
import org.anest.hola.modules.auth.dto.ForgotPasswordRequest;
import org.anest.hola.modules.auth.dto.LoginRequest;
import org.anest.hola.modules.auth.dto.LogoutRequest;
import org.anest.hola.modules.auth.dto.RefreshTokenRequest;
import org.anest.hola.modules.auth.dto.RegisterRequest;
import org.anest.hola.modules.auth.dto.ResendOtpRequest;
import org.anest.hola.modules.auth.dto.ResetPasswordRequest;
import org.anest.hola.modules.auth.dto.VerifyEmailRequest;
import org.anest.hola.modules.auth.entity.EmailVerificationToken;
import org.anest.hola.modules.auth.entity.PasswordResetToken;
import org.anest.hola.modules.auth.entity.RefreshToken;
import org.anest.hola.modules.auth.repository.EmailVerificationTokenRepository;
import org.anest.hola.modules.auth.repository.PasswordResetTokenRepository;
import org.anest.hola.modules.auth.repository.RefreshTokenRepository;
import org.anest.hola.modules.identity.dto.UserAuthDto;
import org.anest.hola.modules.identity.dto.UserResponse;
import org.anest.hola.modules.identity.model.RoleName;
import org.anest.hola.modules.identity.model.UserStatus;
import org.anest.hola.modules.identity.service.UserService;
import org.anest.hola.modules.notification.service.EmailService;
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
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private UserResponse activeUserResponse;
    private UserResponse unverifiedUserResponse;
    private UserResponse lockedUserResponse;

    @BeforeEach
    void setUp() {
        activeUserResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(RoleName.USER))
                .build();

        unverifiedUserResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.UNVERIFIED)
                .roles(Set.of(RoleName.USER))
                .build();

        lockedUserResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.LOCKED)
                .roles(Set.of(RoleName.USER))
                .build();
    }

    @Test
    void register_ShouldCreateUnverifiedUserAndSendVerificationOtp() {
        RegisterRequest request = RegisterRequest.builder()
                .email("  Student@Hola.edu.vn ")
                .password("Password123!")
                .fullName(" Nguyen Van A ")
                .build();

        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
        when(userService.createUser("  Student@Hola.edu.vn ", "hashed-password", " Nguyen Van A "))
                .thenReturn(unverifiedUserResponse);
        when(jwtTokenService.generateNumericOtp()).thenReturn("123456");
        when(jwtTokenService.hashOtp("123456")).thenReturn("hashed-otp");

        UserResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("student@hola.edu.vn");
        assertThat(response.getStatus()).isEqualTo(UserStatus.UNVERIFIED);
        assertThat(response.getRoles()).containsExactly(RoleName.USER);

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(tokenCaptor.getValue().getOtpHash()).isEqualTo("hashed-otp");
        assertThat(tokenCaptor.getValue().getAttempts()).isZero();

        verify(emailService).sendVerificationOtp("student@hola.edu.vn", "Nguyen Van A", "123456", 10L);
    }

    @Test
    void resendVerificationOtp_WhenUserDoesNotExist_ShouldSilentlyReturnWithoutSendingOtp() {
        when(userService.findAuthByEmail("unknown@hola.edu.vn")).thenReturn(Optional.empty());

        authService.resendVerificationOtp(new ResendOtpRequest("unknown@hola.edu.vn"));

        verify(emailVerificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationOtp(any(), any(), any(), any(Long.class));
    }

    @Test
    void resendVerificationOtp_WhenUserIsAlreadyActiveOrLocked_ShouldSilentlyReturnWithoutSendingOtp() {
        UserAuthDto activeAuthDto = UserAuthDto.builder().user(activeUserResponse).build();
        UserAuthDto lockedAuthDto = UserAuthDto.builder().user(lockedUserResponse).build();

        when(userService.findAuthByEmail("active@hola.edu.vn")).thenReturn(Optional.of(activeAuthDto));
        when(userService.findAuthByEmail("locked@hola.edu.vn")).thenReturn(Optional.of(lockedAuthDto));

        authService.resendVerificationOtp(new ResendOtpRequest("active@hola.edu.vn"));
        authService.resendVerificationOtp(new ResendOtpRequest("locked@hola.edu.vn"));

        verify(emailVerificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationOtp(any(), any(), any(), any(Long.class));
    }

    @Test
    void resendVerificationOtp_WhenWithinCooldownWindow_ShouldSilentlyReturnWithoutSendingDuplicateOtp() {
        UserAuthDto unverifiedAuthDto = UserAuthDto.builder().user(unverifiedUserResponse).build();
        EmailVerificationToken recentToken = EmailVerificationToken.builder()
                .id(100L)
                .userId(1L)
                .otpHash("recent-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(9))
                .build();
        recentToken.setCreatedAt(LocalDateTime.now().minusSeconds(20));

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(unverifiedAuthDto));
        when(emailVerificationTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(recentToken));

        authService.resendVerificationOtp(new ResendOtpRequest("student@hola.edu.vn"));

        verify(emailVerificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationOtp(any(), any(), any(), any(Long.class));
    }

    @Test
    void resendVerificationOtp_ForEligibleUnverifiedUser_ShouldIssueAndSendNewOtp() {
        UserAuthDto unverifiedAuthDto = UserAuthDto.builder().user(unverifiedUserResponse).build();
        EmailVerificationToken oldToken = EmailVerificationToken.builder()
                .id(100L)
                .userId(1L)
                .otpHash("old-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        oldToken.setCreatedAt(LocalDateTime.now().minusSeconds(90));

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(unverifiedAuthDto));
        when(emailVerificationTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(oldToken));
        when(jwtTokenService.generateNumericOtp()).thenReturn("654321");
        when(jwtTokenService.hashOtp("654321")).thenReturn("new-hashed-otp");

        authService.resendVerificationOtp(new ResendOtpRequest("student@hola.edu.vn"));

        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationOtp("student@hola.edu.vn", "Nguyen Van A", "654321", 10L);
    }

    @Test
    void verifyEmail_WithValidOtp_ShouldActivateUserAndReturnTokens() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("student@hola.edu.vn")
                .otp("123456")
                .build();

        UserAuthDto unverifiedAuthDto = UserAuthDto.builder()
                .passwordHash("hashed-password")
                .user(unverifiedUserResponse)
                .build();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(100L)
                .userId(1L)
                .otpHash("hashed-otp")
                .attempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(unverifiedAuthDto));
        when(emailVerificationTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(token));
        when(jwtTokenService.hashOtp("123456")).thenReturn("hashed-otp");
        when(userService.activateUser(1L)).thenReturn(activeUserResponse);
        when(jwtTokenService.generateAccessToken(activeUserResponse)).thenReturn("access-token");
        when(jwtTokenService.generateRawRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtTokenService.hashRefreshToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = authService.verifyEmail(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(token.getUsedAt()).isNotNull();
        verify(emailVerificationTokenRepository).save(token);
        verify(userService).activateUser(1L);
    }

    @Test
    void verifyEmail_WithWrongOtp_ShouldIncrementAttemptsAndThrowInvalidOtp() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("student@hola.edu.vn")
                .otp("000000")
                .build();

        UserAuthDto unverifiedAuthDto = UserAuthDto.builder()
                .passwordHash("hashed-password")
                .user(unverifiedUserResponse)
                .build();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(100L)
                .userId(1L)
                .otpHash("hashed-otp")
                .attempts(2)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(unverifiedAuthDto));
        when(emailVerificationTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(token));
        when(jwtTokenService.hashOtp("000000")).thenReturn("wrong-hashed-otp");

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OTP);

        assertThat(token.getAttempts()).isEqualTo(3);
        verify(emailVerificationTokenRepository).save(token);
    }

    @Test
    void verifyEmail_WhenFifthWrongOtpReachesMaxAttempts_ShouldThrowOtpMaxAttemptsExceeded() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("student@hola.edu.vn")
                .otp("000000")
                .build();

        UserAuthDto unverifiedAuthDto = UserAuthDto.builder()
                .passwordHash("hashed-password")
                .user(unverifiedUserResponse)
                .build();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(100L)
                .userId(1L)
                .otpHash("hashed-otp")
                .attempts(4)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(unverifiedAuthDto));
        when(emailVerificationTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(token));
        when(jwtTokenService.hashOtp("000000")).thenReturn("wrong-hashed-otp");

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);

        assertThat(token.getAttempts()).isEqualTo(5);
        verify(emailVerificationTokenRepository).save(token);
    }

    @Test
    void resetPassword_WithWrongOtpOnFifthAttempt_ShouldIncrementAttemptsAndThrowOtpMaxAttemptsExceeded() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("student@hola.edu.vn")
                .otp("999999")
                .newPassword("NewPassword123!")
                .build();

        UserAuthDto activeAuthDto = UserAuthDto.builder()
                .passwordHash("old-hash")
                .user(activeUserResponse)
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(200L)
                .userId(1L)
                .otpHash("expected-otp-hash")
                .attempts(4)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(activeAuthDto));
        when(passwordResetTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(resetToken));
        when(jwtTokenService.hashOtp("999999")).thenReturn("wrong-otp-hash");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);

        assertThat(resetToken.getAttempts()).isEqualTo(5);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    void resetPassword_WithValidOtp_ShouldUpdatePasswordAndRevokeAllRefreshTokens() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("student@hola.edu.vn")
                .otp("654321")
                .newPassword("NewPassword123!")
                .build();

        UserAuthDto activeAuthDto = UserAuthDto.builder()
                .passwordHash("old-hash")
                .user(activeUserResponse)
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(200L)
                .userId(1L)
                .otpHash("expected-otp-hash")
                .attempts(1)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(activeAuthDto));
        when(passwordResetTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(resetToken));
        when(jwtTokenService.hashOtp("654321")).thenReturn("expected-otp-hash");
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hashed-password");

        authService.resetPassword(request);

        assertThat(resetToken.getUsedAt()).isNotNull();
        verify(passwordResetTokenRepository).save(resetToken);
        verify(userService).updatePassword(1L, "new-hashed-password");
        verify(refreshTokenRepository).revokeAllByUserId(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void forgotPassword_ForActiveUser_ShouldSaveResetTokenAndSendEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("student@hola.edu.vn")
                .build();

        UserAuthDto activeAuthDto = UserAuthDto.builder()
                .passwordHash("old-hash")
                .user(activeUserResponse)
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(activeAuthDto));
        when(passwordResetTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());
        when(jwtTokenService.generateNumericOtp()).thenReturn("654321");
        when(jwtTokenService.hashOtp("654321")).thenReturn("hashed-reset-otp");

        authService.forgotPassword(request);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getOtpHash()).isEqualTo("hashed-reset-otp");
        assertThat(captor.getValue().getAttempts()).isZero();
        verify(emailService).sendPasswordResetOtp("student@hola.edu.vn", "Nguyen Van A", "654321", 10L);
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
        when(userService.recordFailedLoginAttempt(eq(1L), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(userService).recordFailedLoginAttempt(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void login_WhenFifthInvalidPasswordReachesThreshold_ShouldThrowAccountTemporarilyLocked() {
        LoginRequest request = LoginRequest.builder()
                .email("student@hola.edu.vn")
                .password("WrongPassword")
                .build();

        UserAuthDto authDto = UserAuthDto.builder()
                .passwordHash("hashed-password")
                .failedLoginAttempts(4)
                .user(activeUserResponse)
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(authDto));
        when(passwordEncoder.matches("WrongPassword", "hashed-password")).thenReturn(false);
        when(userService.recordFailedLoginAttempt(eq(1L), any(LocalDateTime.class))).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
    }

    @Test
    void login_WhenAccountIsTemporarilyLocked_ShouldRejectImmediatelyWithoutCheckingPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("student@hola.edu.vn")
                .password("Password123!")
                .build();

        UserAuthDto lockedAuthDto = UserAuthDto.builder()
                .passwordHash("hashed-password")
                .failedLoginAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(10))
                .user(activeUserResponse)
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(lockedAuthDto));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(userService, never()).recordFailedLoginAttempt(any(), any());
    }

    @Test
    void login_WhenValidAfterPriorFailedAttempts_ShouldCompleteLoginAndResetCounter() {
        LoginRequest request = LoginRequest.builder()
                .email("student@hola.edu.vn")
                .password("Password123!")
                .build();

        UserAuthDto authDto = UserAuthDto.builder()
                .passwordHash("hashed-password")
                .failedLoginAttempts(3)
                .user(activeUserResponse)
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(authDto));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(userService.completeSuccessfulLogin(eq(1L), any(LocalDateTime.class)))
                .thenReturn(activeUserResponse);
        when(jwtTokenService.generateAccessToken(activeUserResponse)).thenReturn("access-token");
        when(jwtTokenService.generateRawRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtTokenService.hashRefreshToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userService).completeSuccessfulLogin(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void login_WhenAccountLocksDuringPasswordCheck_ShouldNotIssueTokens() {
        LoginRequest request = LoginRequest.builder()
                .email("student@hola.edu.vn")
                .password("Password123!")
                .build();
        UserAuthDto authDto = UserAuthDto.builder()
                .passwordHash("hashed-password")
                .user(activeUserResponse)
                .build();

        when(userService.findAuthByEmail("student@hola.edu.vn")).thenReturn(Optional.of(authDto));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(userService.completeSuccessfulLogin(eq(1L), any(LocalDateTime.class)))
                .thenThrow(new AppException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
        verify(refreshTokenRepository, never()).save(any());
        verify(jwtTokenService, never()).generateAccessToken(any());
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
}
