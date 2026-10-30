package org.anest.hola.modules.auth.repository;

import org.anest.hola.modules.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("""
            DELETE FROM EmailVerificationToken evt
            WHERE evt.usedAt IS NOT NULL OR evt.expiresAt <= :cutoff
            """)
    int deleteUsedOrExpired(@Param("cutoff") LocalDateTime cutoff);
}

