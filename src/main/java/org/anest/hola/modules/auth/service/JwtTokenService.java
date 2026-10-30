package org.anest.hola.modules.auth.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.RoleName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;

    @Getter
    @Value("${security.jwt.access-token-expiration-seconds}")
    private long accessTokenExpirationSeconds;

    @Getter
    @Value("${security.jwt.refresh-token-expiration-seconds}")
    private long refreshTokenExpirationSeconds;

    public String generateAccessToken(UserResponse user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenExpirationSeconds);
        List<String> roles = user.getRoles().stream()
                .map(RoleName::name)
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("hola-backend")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String generateRawRefreshToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String generateNumericOtp() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    public String hashOtp(String rawOtp) {
        return hashRefreshToken(rawOtp);
    }

    public String hashRefreshToken(String rawRefreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
