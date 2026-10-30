package org.anest.hola.modules.identity.service;

import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.modules.identity.dto.UserResponse;
import org.anest.hola.modules.identity.entity.Role;
import org.anest.hola.modules.identity.entity.RoleName;
import org.anest.hola.modules.identity.entity.User;
import org.anest.hola.modules.identity.entity.UserStatus;
import org.anest.hola.modules.identity.mapper.UserMapper;
import org.anest.hola.modules.identity.repository.RoleRepository;
import org.anest.hola.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_ShouldNormalizeEmailAndSaveUnverifiedUser() {
        Role userRole = Role.builder().id(1L).roleName(RoleName.USER).build();

        User savedUser = User.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .passwordHash("hashed-pw")
                .fullName("Nguyen Van A")
                .status(UserStatus.UNVERIFIED)
                .roles(Set.of(userRole))
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.UNVERIFIED)
                .roles(Set.of(RoleName.USER))
                .build();

        when(userRepository.existsByEmail("student@hola.edu.vn")).thenReturn(false);
        when(roleRepository.findByRoleName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse result = userService.createUser("  Student@Hola.edu.vn ", "hashed-pw", " Nguyen Van A ");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("student@hola.edu.vn");
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.UNVERIFIED);
        assertThat(userCaptor.getValue().getRoles()).containsExactly(userRole);
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void createUser_WhenEmailExists_ShouldThrowUserAlreadyExists() {
        when(userRepository.existsByEmail("student@hola.edu.vn")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser("Student@Hola.edu.vn", "hashed-pw", "Nguyen Van A"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
    }

    @Test
    void getCurrentUserProfile_WhenUserIsLocked_ShouldThrowAccountInactive() {
        Role userRole = Role.builder().id(1L).roleName(RoleName.USER).build();

        User lockedUser = User.builder()
                .id(1L)
                .email("locked@hola.edu.vn")
                .passwordHash("hashed-pw")
                .fullName("Locked User")
                .status(UserStatus.LOCKED)
                .roles(Set.of(userRole))
                .build();

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> userService.getCurrentUserProfile(1L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }

    @Test
    void getById_WhenUserExists_ShouldReturnUserResponse() {
        Role userRole = Role.builder().id(1L).roleName(RoleName.USER).build();

        User user = User.builder()
                .id(10L)
                .email("user10@hola.edu.vn")
                .passwordHash("hashed-pw")
                .fullName("User 10")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id(10L)
                .email("user10@hola.edu.vn")
                .fullName("User 10")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(RoleName.USER))
                .build();

        when(userRepository.findWithRolesById(10L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse result = userService.getById(10L);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getById_WhenUserDoesNotExist_ShouldThrowUserNotFound() {
        when(userRepository.findWithRolesById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void recordFailedLoginAttempt_WhenBelowLimit_ShouldIncrementCounterWithoutLocking() {
        User user = User.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .failedLoginAttempts(2)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 12, 0);
        boolean locked = userService.recordFailedLoginAttempt(1L, now);

        assertThat(locked).isFalse();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void recordFailedLoginAttempt_WhenReachingFifthAttempt_ShouldLockFor15Minutes() {
        User user = User.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .failedLoginAttempts(4)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 12, 0);
        boolean locked = userService.recordFailedLoginAttempt(1L, now);

        assertThat(locked).isTrue();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isEqualTo(now.plusMinutes(15));
        verify(userRepository).save(user);
    }

    @Test
    void recordFailedLoginAttempt_AfterPreviousLockExpired_ShouldRestartCounterFromOne() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 12, 30);
        User user = User.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .failedLoginAttempts(5)
                .lockedUntil(now.minusMinutes(1))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean locked = userService.recordFailedLoginAttempt(1L, now);

        assertThat(locked).isFalse();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void resetFailedLoginAttempts_ShouldClearCounterAndLockedUntil() {
        User user = User.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .failedLoginAttempts(4)
                .lockedUntil(LocalDateTime.now().minusMinutes(5))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.resetFailedLoginAttempts(1L);

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }
}
