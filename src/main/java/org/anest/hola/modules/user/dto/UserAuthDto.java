package org.anest.hola.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthDto {

    private String passwordHash;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private UserResponse user;

    public boolean isTemporarilyLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}

