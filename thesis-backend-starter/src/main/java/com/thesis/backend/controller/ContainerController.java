
package com.thesis.backend.controller;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.service.ContainerInstanceService;
import com.thesis.backend.service.KubernetesService;
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
}
