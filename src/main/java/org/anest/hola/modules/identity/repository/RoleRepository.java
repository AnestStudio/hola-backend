package org.anest.hola.modules.identity.repository;

import org.anest.hola.modules.identity.entity.Role;
import org.anest.hola.modules.identity.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(RoleName roleName);
}

