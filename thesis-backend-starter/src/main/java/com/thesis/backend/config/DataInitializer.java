package com.thesis.backend.config;

/*
 * Development Demo Credentials:
 * Super Admin: username="superadmin", password="SuperSecure2024!"
 * Teacher: username="teacher", password="TeachSecure2024!"
 * Student: username="student", password="StudyHard2024#"
 * Admin:   username="admin",   password="AdminPower2024$"
 */

import com.thesis.backend.entity.User;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
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
    private final ImageTemplateRepository imageTemplateRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeUsers();
        initializeImageTemplates();
    }

    private void initializeUsers() {
        // Check if users already exist
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping initialization");
            return;
        }

        log.info("Initializing default users...");

        // Create a super admin user
        User superAdmin = new User();
        superAdmin.setUsername("superadmin");
        superAdmin.setEmail("superadmin@university.edu");
        superAdmin.setPassword(passwordEncoder.encode("SuperSecure2024!"));
        superAdmin.setRole("ROLE_SUPER_ADMIN");
        superAdmin.setActive(true);
        userRepository.save(superAdmin);
        log.info("Created super admin user: {}", superAdmin.getUsername());

        // Create a teacher user
        User teacher = new User();
        teacher.setUsername("teacher");
        teacher.setEmail("teacher@university.edu");
        teacher.setPassword(passwordEncoder.encode("TeachSecure2024!"));
        teacher.setRole("ROLE_TEACHER");
        teacher.setActive(true);
        userRepository.save(teacher);
        log.info("Created teacher user: {}", teacher.getUsername());

        // Create a student user
        User student = new User();
        student.setUsername("student");
        student.setEmail("student@university.edu");
        student.setPassword(passwordEncoder.encode("StudyHard2024#"));
        student.setRole("ROLE_STUDENT");
        student.setActive(true);
        userRepository.save(student);
        log.info("Created student user: {}", student.getUsername());

        // Create an admin user (legacy - for backward compatibility)
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@university.edu");
        admin.setPassword(passwordEncoder.encode("AdminPower2024$"));
        admin.setRole("ROLE_ADMIN");
        admin.setActive(true);
        userRepository.save(admin);
        log.info("Created admin user: {}", admin.getUsername());

        log.info("User initialization completed successfully!");
    }

    private void initializeImageTemplates() {
        // Check if templates already exist
        if (imageTemplateRepository.count() > 0) {
            log.info("Image templates already exist, skipping initialization");
            return;
        }

        log.info("Initializing default image templates...");

        // Create sample templates with SSH-enabled images
        ImageTemplate pythonTemplate = ImageTemplate.builder()
                .name("Python Development")
                .dockerImage("thesis-ssh-container:latest")
                .description("Ubuntu with Python development environment and SSH access")
                .build();
        imageTemplateRepository.save(pythonTemplate);

        ImageTemplate nodeTemplate = ImageTemplate.builder()
                .name("Node.js Development")
                .dockerImage("thesis-ssh-container:latest")
                .description("Ubuntu with Node.js development environment and SSH access")
                .build();
        imageTemplateRepository.save(nodeTemplate);

        ImageTemplate ubuntuTemplate = ImageTemplate.builder()
                .name("Ubuntu SSH")
                .dockerImage("thesis-ssh-container:latest")
                .description("Ubuntu 20.04 with SSH server enabled for educational purposes")
                .build();
        imageTemplateRepository.save(ubuntuTemplate);

        log.info("Image template initialization completed successfully!");
    }
}
