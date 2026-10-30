package org.anest.hola.modules.auth.scheduler;

import org.anest.hola.modules.auth.repository.EmailVerificationTokenRepository;
import org.anest.hola.modules.auth.repository.PasswordResetTokenRepository;
import org.anest.hola.modules.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private TokenCleanupScheduler tokenCleanupScheduler;

    @Test
    void purgeRevokedOrExpiredTokens_ShouldCallRepositoryDeleteOnAllTokenTables() {
        when(refreshTokenRepository.deleteRevokedOrExpired(any(LocalDateTime.class))).thenReturn(5);
        when(emailVerificationTokenRepository.deleteUsedOrExpired(any(LocalDateTime.class))).thenReturn(3);
        when(passwordResetTokenRepository.deleteUsedOrExpired(any(LocalDateTime.class))).thenReturn(2);

        int deleted = tokenCleanupScheduler.purgeRevokedOrExpiredTokens();

        assertThat(deleted).isEqualTo(10);
        verify(refreshTokenRepository).deleteRevokedOrExpired(any(LocalDateTime.class));
        verify(emailVerificationTokenRepository).deleteUsedOrExpired(any(LocalDateTime.class));
        verify(passwordResetTokenRepository).deleteUsedOrExpired(any(LocalDateTime.class));
    }
}

