package org.anest.hola.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.anest.hola.modules.user.entity.UserStatus;
import org.anest.hola.modules.user.service.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHi7J8S9E6f/W3Q1xG5y8K9L2M3N4O5P";

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String passwordHash = passwordEncoder.encode(request.getPassword());
        UserResponse createdUser = userService.createUser(
                request.getEmail(),
                passwordHash,
                request.getFullName()
        );
        return issueTokens(createdUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Optional<UserAuthDto> authUserOpt = userService.findAuthByEmail(request.getEmail());
        if (authUserOpt.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), DUMMY_PASSWORD_HASH);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        UserAuthDto authUser = authUserOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), authUser.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        UserResponse user = authUser.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
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
        int deleted = refreshTokenRepository.deleteRevokedOrExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} revoked or expired refresh tokens", deleted);
        }
        return deleted;
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
}
