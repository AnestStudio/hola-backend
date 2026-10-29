package org.anest.hola.modules.auth.repository;

import org.anest.hola.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revoked = true, rt.revokedAt = :now, rt.updatedAt = :now
            WHERE rt.id = :id AND rt.revoked = false AND rt.expiresAt > :now
            """)
    int revokeIfActive(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revoked = true, rt.revokedAt = :now, rt.updatedAt = :now
            WHERE rt.tokenHash = :tokenHash AND rt.revoked = false
            """)
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            DELETE FROM RefreshToken rt
            WHERE rt.revoked = true OR rt.expiresAt <= :cutoff
            """)
    int deleteRevokedOrExpired(@Param("cutoff") LocalDateTime cutoff);
}
