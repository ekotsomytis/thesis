package com.thesis.backend.controller;

import com.thesis.backend.entity.User;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.service.NamespaceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NamespaceService namespaceService;

    @Data
    public static class CreateUserRequest {
        private String username;
        private String email;
        private String password;
        private String role; // ROLE_TEACHER, ROLE_STUDENT
    }

    @Data
    public static class UpdateUserRequest {
        private String email;
        private String password;
        private Boolean active;
    }

    /**
     * Get all users (Super Admin only)
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            // Remove password from response
            users.forEach(user -> user.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Failed to fetch all users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new user (Super Admin only)
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request, 
                                       @AuthenticationPrincipal User admin) {
        try {
            // Validate role
            if (!request.getRole().equals("ROLE_TEACHER") && !request.getRole().equals("ROLE_STUDENT")) {
                return ResponseEntity.badRequest().body("Invalid role. Must be ROLE_TEACHER or ROLE_STUDENT");
            }

            // Check if username or email already exists
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            // Create user
            User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .createdByAdminId(admin.getId())
                .build();

            User savedUser = userRepository.save(user);

            // Create namespace for student
            if ("ROLE_STUDENT".equals(request.getRole())) {
                try {
                    String namespace = namespaceService.createStudentNamespace(savedUser);
                    savedUser.setKubernetesNamespace(namespace);
                    savedUser = userRepository.save(savedUser);
                    log.info("Created namespace {} for student {}", namespace, savedUser.getUsername());
                } catch (Exception e) {
                    log.error("Failed to create namespace for student: {}", savedUser.getUsername(), e);
                    // Continue anyway, namespace can be created later
                }
            }

            // Remove password from response
            savedUser.setPassword(null);
            return ResponseEntity.ok(savedUser);

        } catch (Exception e) {
            log.error("Failed to create user", e);
            return ResponseEntity.badRequest().body("Failed to create user: " + e.getMessage());
        }
    }

    /**
     * Update a user (Super Admin only)
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                       @RequestBody UpdateUserRequest request) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Update fields
            if (request.getEmail() != null) {
                // Check if email already exists for other users
                userRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(id)) {
                            throw new RuntimeException("Email already exists");
                        }
                    });
                user.setEmail(request.getEmail());
            }

            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            if (request.getActive() != null) {
                user.setActive(request.getActive());
                
                // If deactivating a student, cleanup their resources
                if (!request.getActive() && "ROLE_STUDENT".equals(user.getRole())) {
                    try {
                        namespaceService.cleanupStudentResources(user);
                    } catch (Exception e) {
                        log.error("Failed to cleanup resources for deactivated student: {}", user.getUsername(), e);
                    }
                }
            }

            User updatedUser = userRepository.save(user);
            updatedUser.setPassword(null);
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            log.error("Failed to update user", e);
            return ResponseEntity.badRequest().body("Failed to update user: " + e.getMessage());
        }
    }

    /**
     * Delete a user (Super Admin only)
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Don't allow deletion of super admins
            if ("ROLE_SUPER_ADMIN".equals(user.getRole())) {
                return ResponseEntity.badRequest().body("Cannot delete super admin users");
            }

            // Clean up student namespace if it's a student
            if ("ROLE_STUDENT".equals(user.getRole())) {
                try {
                    namespaceService.deleteStudentNamespace(user);
                } catch (Exception e) {
                    log.error("Failed to delete namespace for student: {}", user.getUsername(), e);
                    // Continue with user deletion even if namespace cleanup fails
                }
            }

            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));

        } catch (Exception e) {
            log.error("Failed to delete user", e);
            return ResponseEntity.badRequest().body("Failed to delete user: " + e.getMessage());
        }
    }

    /**
     * Get user statistics (Super Admin only)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            List<User> users = userRepository.findAll();
            
            long totalUsers = users.size();
            long teachers = users.stream().filter(u -> "ROLE_TEACHER".equals(u.getRole())).count();
            long students = users.stream().filter(u -> "ROLE_STUDENT".equals(u.getRole())).count();
            long admins = users.stream().filter(u -> "ROLE_SUPER_ADMIN".equals(u.getRole())).count();
            long activeUsers = users.stream().filter(u -> Boolean.TRUE.equals(u.getActive())).count();

            Map<String, Object> stats = Map.of(
                "totalUsers", totalUsers,
                "teachers", teachers,
                "students", students,
                "admins", admins,
                "activeUsers", activeUsers,
                "inactiveUsers", totalUsers - activeUsers
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to fetch user statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Reset user password (Super Admin only)
     */
    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id, 
                                              @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("New password is required");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));

        } catch (Exception e) {
            log.error("Failed to reset user password", e);
            return ResponseEntity.badRequest().body("Failed to reset password: " + e.getMessage());
        }
    }
}
