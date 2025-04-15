package com.univers.univers_backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import com.univers.univers_backend.Entity.Role;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Dotenv dotenv;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, Dotenv dotenv){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dotenv = dotenv;
    }

    @Override
    public void run(ApplicationArguments args){
        String adminEmail = dotenv.get("ADMIN_EMAIL", "admin@univers.com");
        String adminPassword = dotenv.get("ADMIN_PASSWORD", "admin1234");
        Role adminRole = Role.SUPER_ADMIN;

        if(userRepository.existsByEmail(adminEmail) || userRepository.existsByRoles(adminRole)) return;

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFirstname("System");
        admin.setLastname("Admin");
        admin.setRoles(Role.SUPER_ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
        System.out.println("Admin user created with email: " + adminEmail);

    }
}
