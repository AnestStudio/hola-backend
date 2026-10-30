package org.anest.hola.modules.auth.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.anest.hola.modules.auth.repository.EmailVerificationTokenRepository;
import org.anest.hola.modules.auth.repository.PasswordResetTokenRepository;
import org.anest.hola.modules.auth.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

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
}

