package com.ems.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ems.entity.User;
import com.ems.repository.UserRepository;

@Configuration
public class AdminInitializer {


@Bean
public CommandLineRunner createInitialAdmin(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder) {

    return args -> {

        if (userRepository.count() == 0) {

            User admin = new User();

            admin.setUsername("admin");
            admin.setEmail("admin@example.com");

            // Change this password before running the application
            admin.setPassword(
                passwordEncoder.encode("Admin@12345")
            );

            admin.setRole("ADMIN");

            userRepository.save(admin);

            System.out.println("Initial admin account created!");
        }
    };
}


}
