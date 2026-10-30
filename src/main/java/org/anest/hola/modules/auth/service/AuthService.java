package org.anest.hola.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.anest.hola.modules.user.dto.UserAuthDto;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.UserStatus;
import org.anest.hola.modules.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final int DEFAULT_OTP_MAX_ATTEMPTS = 5;
    public static final long DEFAULT_OTP_EXPIRATION_MINUTES = 10L;

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHi7J8S9E6f/W3Q1xG5y8K9L2M3N4O5P";

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final EmailService emailService;

    @Value("${security.otp.expiration-minutes:10}")
    private long otpExpirationMinutes = DEFAULT_OTP_EXPIRATION_MINUTES;

    @Value("${security.otp.max-attempts:5}")
    private int otpMaxAttempts = DEFAULT_OTP_MAX_ATTEMPTS;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String passwordHash = passwordEncoder.encode(request.getPassword());
        UserResponse createdUser = userService.createUser(
                request.getEmail(),
                passwordHash,
                request.getFullName()
        );
        issueVerificationOtp(createdUser);
        return createdUser;
    }

    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        UserAuthDto authUser = userService.findAuthByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        UserResponse user = authUser.getUser();
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_VERIFIED);
        }
        if (user.getStatus() != UserStatus.UNVERIFIED) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        EmailVerificationToken token = emailVerificationTokenRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        LocalDateTime now = LocalDateTime.now();
        if (token.isExpired(now)) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        if (token.isMaxAttemptsReached(otpMaxAttempts)) {
            throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        String inputHash = jwtTokenService.hashOtp(request.getOtp().trim());
        if (!constantTimeEquals(inputHash, token.getOtpHash())) {
            int attempts = token.incrementAttempts();
            emailVerificationTokenRepository.save(token);
            if (attempts >= otpMaxAttempts) {
                throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
            }
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        token.setUsedAt(now);
        emailVerificationTokenRepository.save(token);

        UserResponse activatedUser = userService.activateUser(user.getId());
        return issueTokens(activatedUser);
    }

    @Transactional
    public void resendVerificationOtp(ResendOtpRequest request) {
        UserAuthDto authUser = userService.findAuthByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserResponse user = authUser.getUser();
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_VERIFIED);
        }
        if (user.getStatus() != UserStatus.UNVERIFIED) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        issueVerificationOtp(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<UserAuthDto> authUserOpt = userService.findAuthByEmail(request.getEmail());
        if (authUserOpt.isEmpty()) {
            return;
        }

        UserResponse user = authUserOpt.get().getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            return;
        }

        String rawOtp = jwtTokenService.generateNumericOtp();
        String otpHash = jwtTokenService.hashOtp(rawOtp);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .otpHash(otpHash)
                .attempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetOtp(user.getEmail(), user.getFullName(), rawOtp, otpExpirationMinutes);
    }

    @Transactional(noRollbackFor = AppException.class)
    public void resetPassword(ResetPasswordRequest request) {
        UserAuthDto authUser = userService.findAuthByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        UserResponse user = authUser.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        LocalDateTime now = LocalDateTime.now();
        if (token.isExpired(now)) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        if (token.isMaxAttemptsReached(otpMaxAttempts)) {
            throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        String inputHash = jwtTokenService.hashOtp(request.getOtp().trim());
        if (!constantTimeEquals(inputHash, token.getOtpHash())) {
            int attempts = token.incrementAttempts();
            passwordResetTokenRepository.save(token);
            if (attempts >= otpMaxAttempts) {
                throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
            }
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        token.setUsedAt(now);
        passwordResetTokenRepository.save(token);

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        userService.updatePassword(user.getId(), newPasswordHash);
        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
    }

    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse login(LoginRequest request) {
        Optional<UserAuthDto> authUserOpt = userService.findAuthByEmail(request.getEmail());
        if (authUserOpt.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), DUMMY_PASSWORD_HASH);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        UserAuthDto authUser = authUserOpt.get();
        LocalDateTime now = LocalDateTime.now();
        if (authUser.isTemporarilyLocked(now)) {
            throw new AppException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
        }

        if (authUser.getPasswordHash() == null) {
            passwordEncoder.matches(request.getPassword(), DUMMY_PASSWORD_HASH);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        UserResponse user = authUser.getUser();
        if (!passwordEncoder.matches(request.getPassword(), authUser.getPasswordHash())) {
            boolean lockedNow = userService.recordFailedLoginAttempt(user.getId(), now);
            if (lockedNow) {
                throw new AppException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
            }
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() == UserStatus.UNVERIFIED) {
            throw new AppException(ErrorCode.ACCOUNT_UNVERIFIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (authUser.getFailedLoginAttempts() > 0 || authUser.getLockedUntil() != null) {
            userService.resetFailedLoginAttempts(user.getId());
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = jwtTokenService.hashRefreshToken(request.getRefreshToken().trim());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        LocalDateTime now = LocalDateTime.now();
        if (storedToken.isRevoked() || storedToken.isExpired(now)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        int updatedRows = refreshTokenRepository.revokeIfActive(storedToken.getId(), now);
        if (updatedRows != 1) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserResponse user = userService.findById(storedToken.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = jwtTokenService.hashRefreshToken(request.getRefreshToken().trim());
        refreshTokenRepository.revokeByTokenHash(tokenHash, LocalDateTime.now());
    }

    @Transactional
    @Scheduled(cron = "${security.jwt.cleanup-cron}")
    public int purgeRevokedOrExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedRefresh = refreshTokenRepository.deleteRevokedOrExpired(now);
        int deletedEmailOtp = emailVerificationTokenRepository.deleteUsedOrExpired(now);
        int deletedPasswordOtp = passwordResetTokenRepository.deleteUsedOrExpired(now);
        int totalDeleted = deletedRefresh + deletedEmailOtp + deletedPasswordOtp;
        if (totalDeleted > 0) {
            log.info(
                    "Purged expired/used auth tokens (refresh={}, emailVerification={}, passwordReset={})",
                    deletedRefresh, deletedEmailOtp, deletedPasswordOtp
            );
        }
        return totalDeleted;
    }

    private AuthResponse issueTokens(UserResponse user) {
        String accessToken = jwtTokenService.generateAccessToken(user);
        String rawRefreshToken = jwtTokenService.generateRawRefreshToken();
        String refreshTokenHash = jwtTokenService.hashRefreshToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshTokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenService.getRefreshTokenExpirationSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getAccessTokenExpirationSeconds())
                .user(user)
                .build();
    }

    private void issueVerificationOtp(UserResponse user) {
        String rawOtp = jwtTokenService.generateNumericOtp();
        String otpHash = jwtTokenService.hashOtp(rawOtp);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .userId(user.getId())
                .otpHash(otpHash)
                .attempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationOtp(user.getEmail(), user.getFullName(), rawOtp, otpExpirationMinutes);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
