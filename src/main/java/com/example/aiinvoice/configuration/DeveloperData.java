package com.example.aiinvoice.configuration;

import com.example.security.entity.Role;
import com.example.security.entity.UserWithRoles;
import com.example.security.repository.UserWithRolesRepository;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;



@Configuration
public class DeveloperData implements ApplicationRunner {

    @Autowired
    UserWithRolesRepository userWithRolesRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String password = System.getenv("SWIFTDK_PASSWORD");
       UserWithRoles user = new UserWithRoles("SwiftDK", password, Role.ADMIN);

       userWithRolesRepository.save(user);


    }
}
