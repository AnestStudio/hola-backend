package org.anest.hola.modules.user.service;

import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.Role;
import org.anest.hola.modules.user.entity.User;
import org.anest.hola.modules.user.entity.UserStatus;
import org.anest.hola.modules.user.mapper.UserMapper;
import org.anest.hola.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_ShouldNormalizeEmailAndSaveActiveUser() {
        User savedUser = User.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .passwordHash("hashed-pw")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .email("student@hola.edu.vn")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .build();

        when(userRepository.existsByEmail("student@hola.edu.vn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse result = userService.createUser("  Student@Hola.edu.vn ", "hashed-pw", " Nguyen Van A ");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("student@hola.edu.vn");
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userCaptor.getValue().getRoles()).containsExactly(Role.USER);
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
        User lockedUser = User.builder()
                .id(1L)
                .email("locked@hola.edu.vn")
                .passwordHash("hashed-pw")
                .fullName("Locked User")
                .status(UserStatus.LOCKED)
                .roles(Set.of(Role.USER))
                .build();

        when(userRepository.findWithRolesById(1L)).thenReturn(java.util.Optional.of(lockedUser));

        assertThatThrownBy(() -> userService.getCurrentUserProfile(1L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }
}

