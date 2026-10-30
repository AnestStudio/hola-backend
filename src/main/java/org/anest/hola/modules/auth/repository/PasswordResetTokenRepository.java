package org.anest.hola.modules.auth.repository;

import org.anest.hola.modules.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("""
            DELETE FROM PasswordResetToken prt
            WHERE prt.usedAt IS NOT NULL OR prt.expiresAt <= :cutoff
            """)
    int deleteUsedOrExpired(@Param("cutoff") LocalDateTime cutoff);
}

