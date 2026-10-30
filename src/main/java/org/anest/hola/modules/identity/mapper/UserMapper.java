package org.anest.hola.modules.identity.mapper;

import org.anest.hola.modules.identity.dto.UserResponse;
import org.anest.hola.modules.identity.entity.Role;
import org.anest.hola.modules.identity.entity.RoleName;
import org.anest.hola.modules.identity.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    default RoleName mapRole(Role role) {
        return role == null ? null : role.getRoleName();
    }
}
