package org.anest.hola.modules.user.service;

import lombok.RequiredArgsConstructor;
import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.modules.user.dto.UserAuthDto;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.Role;
import org.anest.hola.modules.user.entity.User;
import org.anest.hola.modules.user.entity.UserStatus;
import org.anest.hola.modules.user.mapper.UserMapper;
import org.anest.hola.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
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
                        .user(userMapper.toResponse(user))
                        .build());
    }

    public Optional<UserResponse> findById(Long id) {
        return userRepository.findWithRolesById(id)
                .map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse createUser(String email, String passwordHash, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .fullName(fullName.trim())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
