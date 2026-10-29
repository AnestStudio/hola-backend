package org.anest.hola.modules.user.service;

import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.Role;
import org.anest.hola.modules.user.entity.RoleName;
import org.anest.hola.modules.user.entity.User;
import org.anest.hola.modules.user.entity.UserStatus;
import org.anest.hola.modules.user.mapper.UserMapper;
import org.anest.hola.modules.user.repository.RoleRepository;
import org.anest.hola.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void createUser_ShouldNormalizeEmailAndSaveActiveUser() {
        Role userRole = Role.builder().id(1L).roleName(RoleName.USER).build();

        User savedUser = User.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .passwordHash("hashed-pw")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
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
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
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
}
