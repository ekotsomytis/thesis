package com.thesis.backend.controller;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NamespaceService namespaceService;
    private final ContainerInstanceRepository containerInstanceRepository;

    /**
     * Get all users (for admins and teachers)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            // Create safe user representations without passwords
            List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("role", user.getRole());
                    userInfo.put("status", "active"); // You can add a status field to User entity later
                    return userInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userList);
        } catch (Exception e) {
            log.error("Failed to fetch users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get only students (for teachers)
     */
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllStudents() {
        try {
            List<User> students = userRepository.findByRole("ROLE_STUDENT");
            
            List<Map<String, Object>> studentList = students.stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("role", user.getRole());
                    userInfo.put("status", "active");
                    return userInfo;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(studentList);
        } catch (Exception e) {
            log.error("Failed to fetch students", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user statistics for dashboard
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            List<User> allUsers = userRepository.findAll();
            
            long totalUsers = allUsers.size();
            long students = allUsers.stream()
                .filter(user -> "ROLE_STUDENT".equals(user.getRole()))
                .count();
            long teachers = allUsers.stream()
                .filter(user -> "ROLE_TEACHER".equals(user.getRole()))
                .count();
            long admins = allUsers.stream()
                .filter(user -> "ROLE_ADMIN".equals(user.getRole()))
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalUsers);
            stats.put("students", students);
            stats.put("teachers", teachers);
            stats.put("admins", admins);
            stats.put("active", totalUsers); // Assuming all users are active for now
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to fetch user statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        try {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            userInfo.put("kubernetesNamespace", user.getKubernetesNamespace());
            userInfo.put("status", "active");
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Failed to fetch current user info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new user (admin only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request, @AuthenticationPrincipal User admin) {
        try {
            log.info("Admin {} creating new user: {}", admin.getUsername(), request.getUsername());
            
            // Validate input
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }
            if (request.getRole() == null || request.getRole().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Role is required");
            }
            
            // Check if username already exists
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            
            // Validate role
            String role = request.getRole().toUpperCase();
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }
            
            if (!role.equals("ROLE_STUDENT") && !role.equals("ROLE_TEACHER") && 
                !role.equals("ROLE_ADMIN") && !role.equals("ROLE_SUPER_ADMIN")) {
                return ResponseEntity.badRequest().body("Invalid role. Must be: STUDENT, TEACHER, ADMIN, or SUPER_ADMIN");
            }
            
            // Create user
            User newUser = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail() != null ? request.getEmail() : request.getUsername() + "@example.com")
                    .role(role)
                    .build();
            
            // Create Kubernetes namespace for students
            if ("ROLE_STUDENT".equals(role)) {
                String namespaceName = "student-" + request.getUsername().toLowerCase();
                try {
                    Map<String, String> labels = new HashMap<>();
                    labels.put("type", "student-namespace");
                    labels.put("student", request.getUsername());
                    labels.put("managed-by", "thesis-backend");
                    
                    namespaceService.createNamespace(namespaceName, labels);
                    newUser.setKubernetesNamespace(namespaceName);
                    log.info("Created Kubernetes namespace: {}", namespaceName);
                } catch (Exception e) {
                    log.warn("Failed to create Kubernetes namespace for student {}: {}", 
                            request.getUsername(), e.getMessage());
                    // Continue anyway, namespace can be created later
                }
            }
            
            // Save user
            User savedUser = userRepository.save(newUser);
            
            // Return user info (without password)
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", savedUser.getId());
            userInfo.put("username", savedUser.getUsername());
            userInfo.put("email", savedUser.getEmail());
            userInfo.put("role", savedUser.getRole());
            userInfo.put("kubernetesNamespace", savedUser.getKubernetesNamespace());
            userInfo.put("status", "active");
            
            log.info("User {} created successfully by admin {}", savedUser.getUsername(), admin.getUsername());
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return ResponseEntity.badRequest().body("Failed to create user: " + e.getMessage());
        }
    }

    /**
     * Get user by ID (admin only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            userInfo.put("kubernetesNamespace", user.getKubernetesNamespace());
            userInfo.put("status", "active");
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Failed to fetch user {}", id, e);
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    /**
     * Update user (admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id, 
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User admin) {
        try {
            log.info("Admin {} updating user {}", admin.getUsername(), id);
            
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Update fields if provided
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }
            
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            
            if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
                String role = request.getRole().toUpperCase();
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }
                user.setRole(role);
            }
            
            User updatedUser = userRepository.save(user);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", updatedUser.getId());
            userInfo.put("username", updatedUser.getUsername());
            userInfo.put("email", updatedUser.getEmail());
            userInfo.put("role", updatedUser.getRole());
            userInfo.put("kubernetesNamespace", updatedUser.getKubernetesNamespace());
            userInfo.put("status", "active");
            
            log.info("User {} updated successfully by admin {}", updatedUser.getUsername(), admin.getUsername());
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            log.error("Failed to update user {}", id, e);
            return ResponseEntity.badRequest().body("Failed to update user: " + e.getMessage());
        }
    }

    /**
     * Delete user (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        try {
            log.info("Admin {} deleting user {}", admin.getUsername(), id);
            
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Prevent deleting yourself
            if (user.getId().equals(admin.getId())) {
                return ResponseEntity.badRequest().body("Cannot delete your own account");
            }
            
            // Prevent deleting super admin if you're not super admin
            if ("ROLE_SUPER_ADMIN".equals(user.getRole()) && !"ROLE_SUPER_ADMIN".equals(admin.getRole())) {
                return ResponseEntity.badRequest().body("Only super admins can delete super admin accounts");
            }
            
            String username = user.getUsername();
            String namespace = user.getKubernetesNamespace();
            
            // First, delete all containers owned by this user
            List<ContainerInstance> userContainers = containerInstanceRepository.findByOwner(user);
            if (!userContainers.isEmpty()) {
                log.info("Deleting {} containers for user {}", userContainers.size(), username);
                containerInstanceRepository.deleteAll(userContainers);
                log.info("Deleted {} containers", userContainers.size());
            }
            
            // Delete user from database
            userRepository.delete(user);
            
            // Clean up Kubernetes namespace if exists
            if (namespace != null && !namespace.isEmpty()) {
                try {
                    namespaceService.deleteNamespace(namespace);
                    log.info("Deleted Kubernetes namespace: {}", namespace);
                } catch (Exception e) {
                    log.warn("Failed to delete Kubernetes namespace {}: {}", namespace, e.getMessage());
                    // Continue anyway, manual cleanup might be needed
                }
            }
            
            log.info("User {} deleted successfully by admin {}", username, admin.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            response.put("username", username);
            response.put("containersDeleted", userContainers.size());
            if (namespace != null) {
                response.put("deletedNamespace", namespace);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to delete user {}", id, e);
            return ResponseEntity.badRequest().body("Failed to delete user: " + e.getMessage());
        }
    }

    // DTOs
    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String email;
        private String role; // STUDENT, TEACHER, ADMIN, SUPER_ADMIN
    }

    @Data
    public static class UpdateUserRequest {
        private String email;
        private String password;
        private String role;
    }
}
