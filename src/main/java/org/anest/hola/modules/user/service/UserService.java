package org.anest.hola.modules.user.service;

import lombok.RequiredArgsConstructor;
import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.modules.user.dto.UserAuthDto;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.Role;
import org.anest.hola.modules.user.entity.RoleName;
import org.anest.hola.modules.user.entity.User;
import org.anest.hola.modules.user.entity.UserStatus;
import org.anest.hola.modules.user.mapper.UserMapper;
import org.anest.hola.modules.user.repository.RoleRepository;
import org.anest.hola.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserResponse getById(Long id) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    public UserResponse getCurrentUserProfile(Long userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }
        return userMapper.toResponse(user);
    }

    public Optional<UserAuthDto> findAuthByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .map(user -> UserAuthDto.builder()
                        .passwordHash(user.getPasswordHash())
                        .failedLoginAttempts(user.getFailedLoginAttempts())
                        .lockedUntil(user.getLockedUntil())
                        .user(userMapper.toResponse(user))
                        .build());
    }

    public Optional<UserResponse> findById(Long id) {
        return userRepository.findWithRolesById(id)
                .map(userMapper::toResponse);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailedLoginAttempt(Long userId, LocalDateTime now) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        int currentAttempts = user.getFailedLoginAttempts();
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            currentAttempts = 0;
            user.setLockedUntil(null);
        }

        int nextAttempts = currentAttempts + 1;
        user.setFailedLoginAttempts(nextAttempts);

        boolean locked = false;
        if (nextAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(now.plusMinutes(LOCK_DURATION_MINUTES));
            locked = true;
        }

        userRepository.save(user);
        return locked;
    }

    @Transactional
    public void resetFailedLoginAttempts(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        });
    }

    @Transactional
    public UserResponse createUser(String email, String passwordHash, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        Role defaultRole = roleRepository.findByRoleName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.USER).build()));

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .fullName(fullName.trim())
                .status(UserStatus.UNVERIFIED)
                .roles(new HashSet<>(Set.of(defaultRole)))
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse activateUser(Long userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public void updatePassword(Long userId, String newPasswordHash) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setPasswordHash(newPasswordHash);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
