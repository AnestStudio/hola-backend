package org.anest.hola.modules.user.mapper;

import org.anest.hola.modules.user.dto.UserResponse;
import org.anest.hola.modules.user.entity.Role;
import org.anest.hola.modules.user.entity.RoleName;
import org.anest.hola.modules.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    default RoleName mapRole(Role role) {
        return role == null ? null : role.getRoleName();
    }
}
