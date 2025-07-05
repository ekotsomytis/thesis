package com.thesis.backend.controller;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.service.ContainerInstanceService;
import com.thesis.backend.service.KubernetesService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final KubernetesService kubeService;
    private final ContainerInstanceRepository containerRepo;
    private final ImageTemplateRepository imageRepo;
    private final ContainerInstanceService containerInstanceService;

    @Data
    public static class CreateContainerRequest {
        private Long imageId;  // Changed from templateId to imageId
        private Long studentId;
    }

    @PostMapping("/create/{imageId}")
    public ResponseEntity<?> create(@PathVariable Long imageId, @RequestParam String username) {
        ImageTemplate template = imageRepo.findById(imageId).orElseThrow();
        String podName = kubeService.createContainer(template.getDockerImage(), username);
        ContainerInstance instance = ContainerInstance.builder()
                .kubernetesPodName(podName)
                .status("Running")
                .name(podName)
                .build();
        return ResponseEntity.ok(containerRepo.save(instance));
    }

    /**
     * Create a container for a student (teacher only)
     */
    @PostMapping("/create-for-student")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> createContainerForStudent(
            @RequestBody CreateContainerRequest request,
            @AuthenticationPrincipal User teacher) {
        try {
            log.info("Teacher {} creating container for student {} using image {}", 
                    teacher.getUsername(), request.getStudentId(), request.getImageId());
            
            ContainerInstance instance = containerInstanceService.createContainerForStudent(
                    request.getImageId(), request.getStudentId(), teacher);
            
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            log.error("Failed to create container for student", e);
            return ResponseEntity.badRequest().body("Failed to create container: " + e.getMessage());
        }
    }

    /**
     * Get all containers (for teachers and admins)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<ContainerInstance>> getAllContainers() {
        try {
            List<ContainerInstance> containers = containerInstanceService.getAllContainers();
            return ResponseEntity.ok(containers);
        } catch (Exception e) {
            log.error("Failed to fetch all containers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get containers for the authenticated user (students see only their own)
     */
    @GetMapping("/my-containers")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<List<ContainerInstance>> getMyContainers(@AuthenticationPrincipal User user) {
        try {
            List<ContainerInstance> containers;
            if ("ROLE_TEACHER".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole())) {
                // Teachers can see all containers
                containers = containerInstanceService.getAllContainers();
            } else {
                // Students see only their own containers
                containers = containerInstanceService.getStudentContainers(user);
            }
            return ResponseEntity.ok(containers);
        } catch (Exception e) {
            log.error("Failed to fetch user containers for user: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get container statistics for dashboard
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getContainerStats() {
        try {
            List<ContainerInstance> containers = containerInstanceService.getAllContainers();
            
            long totalContainers = containers.size();
            long runningContainers = containers.stream()
                .filter(c -> "Running".equalsIgnoreCase(c.getStatus()))
                .count();
            long stoppedContainers = containers.stream()
                .filter(c -> "Stopped".equalsIgnoreCase(c.getStatus()))
                .count();
            long pendingContainers = containers.stream()
                .filter(c -> "Pending".equalsIgnoreCase(c.getStatus()))
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalContainers);
            stats.put("running", runningContainers);
            stats.put("stopped", stoppedContainers);
            stats.put("pending", pendingContainers);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to fetch container statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{id}/refresh-status")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> refreshContainerStatus(@PathVariable Long id) {
        try {
            ContainerInstance container = containerRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Container not found"));
            
            // Update container status
            containerInstanceService.updateContainerStatus(container);
            
            return ResponseEntity.ok(container);
        } catch (Exception e) {
            log.error("Failed to refresh container status", e);
            return ResponseEntity.badRequest().body("Failed to refresh status: " + e.getMessage());
        }
    }
    
    @PostMapping("/refresh-all-statuses")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> refreshAllContainerStatuses() {
        try {
            List<ContainerInstance> containers = containerRepo.findAll();
            
            for (ContainerInstance container : containers) {
                containerInstanceService.updateContainerStatus(container);
            }
            
            return ResponseEntity.ok(Map.of("updated", containers.size()));
        } catch (Exception e) {
            log.error("Failed to refresh all container statuses", e);
            return ResponseEntity.badRequest().body("Failed to refresh statuses: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}/ssh-info")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or @containerInstanceService.canStudentAccessContainer(#id, authentication.name)")
    public ResponseEntity<?> getSshInfo(@PathVariable Long id) {
        try {
            ContainerInstance container = containerRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Container not found"));
            
            // Generate SSH connection details
            Map<String, Object> sshInfo = new HashMap<>();
            sshInfo.put("containerId", container.getId());
            sshInfo.put("containerName", container.getName());
            sshInfo.put("status", container.getStatus());
            sshInfo.put("podName", container.getKubernetesPodName());
            
            if ("Running".equals(container.getStatus())) {
                // In development mode, provide simulated SSH details
                sshInfo.put("host", "localhost");
                sshInfo.put("port", 2200 + container.getId().intValue()); // Use unique port per container
                sshInfo.put("username", "root");
                sshInfo.put("password", "student123");
                sshInfo.put("dockerImage", container.getImageTemplate().getDockerImage());
                sshInfo.put("ready", true);
                sshInfo.put("instructions", "Use these credentials to connect via SSH. In a real deployment, this would connect to the actual container.");
            } else {
                sshInfo.put("ready", false);
                sshInfo.put("message", "Container is not running yet. Please wait for it to start.");
            }
            
            return ResponseEntity.ok(sshInfo);
        } catch (Exception e) {
            log.error("Failed to get SSH info", e);
            return ResponseEntity.badRequest().body("Failed to get SSH info: " + e.getMessage());
        }
    }
}
