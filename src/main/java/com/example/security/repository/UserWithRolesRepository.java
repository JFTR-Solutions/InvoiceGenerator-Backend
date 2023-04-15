package com.example.security.repository;


import com.example.security.entity.UserWithRoles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface
UserWithRolesRepository extends JpaRepository<UserWithRoles,String> {

    UserWithRoles findByUsername(String username);
}
