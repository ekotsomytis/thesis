package com.thesis.backend.config;

import com.thesis.backend.entity.User;
import com.thesis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeUsers();
    }

    private void initializeUsers() {
        // Check if users already exist
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping initialization");
            return;
        }

        log.info("Initializing default users...");

        // Create a teacher user
        User teacher = new User();
        teacher.setUsername("teacher");
        teacher.setEmail("teacher@university.edu");
        teacher.setPassword(passwordEncoder.encode("teacher123"));
        teacher.setRole("TEACHER");
        userRepository.save(teacher);
        log.info("Created teacher user: {}", teacher.getUsername());

        // Create a student user
        User student = new User();
        student.setUsername("student");
        student.setEmail("student@university.edu");
        student.setPassword(passwordEncoder.encode("student123"));
        student.setRole("STUDENT");
        userRepository.save(student);
        log.info("Created student user: {}", student.getUsername());

        // Create an admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@university.edu");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        userRepository.save(admin);
        log.info("Created admin user: {}", admin.getUsername());

        log.info("User initialization completed successfully!");
    }
}
