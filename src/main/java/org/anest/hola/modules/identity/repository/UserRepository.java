package org.anest.hola.modules.identity.repository;

import jakarta.persistence.LockModeType;
import org.anest.hola.modules.identity.entity.User;
import org.anest.hola.modules.identity.model.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.failedLoginAttempts = 0, u.lockedUntil = null, u.updatedAt = :now
            WHERE u.id = :id AND u.status = :status
              AND (u.lockedUntil IS NULL OR u.lockedUntil <= :now)
            """)
    int completeSuccessfulLogin(@Param("id") Long id, @Param("status") UserStatus status,
                                @Param("now") LocalDateTime now);

    boolean existsByEmail(String email);
}
