package com.thesis.backend.controller;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.repository.UserRepository;
import com.thesis.backend.service.ContainerInstanceService;
import com.thesis.backend.service.KubernetesService;
import com.thesis.backend.service.NamespaceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    private final UserRepository userRepository;
    private final NamespaceService namespaceService;

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
     * Create a container for a student (teacher/admin only)
     */
    @PostMapping("/create-for-student")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
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
     * Create a container for self (student creating their own container)
     */
    @PostMapping("/create-for-self")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createContainerForSelf(
            @RequestBody CreateContainerRequest request,
            @AuthenticationPrincipal User student) {
        try {
            log.info("Student {} creating container for themselves using image {}", 
                    student.getUsername(), request.getImageId());
            
            // Student creates container for themselves, so studentId = student's own ID
            ContainerInstance instance = containerInstanceService.createContainerForStudent(
                    request.getImageId(), student.getId(), student);
            
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            log.error("Failed to create container for self", e);
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
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<ContainerInstance>> getMyContainers(@AuthenticationPrincipal User user) {
        try {
            List<ContainerInstance> containers;
            if ("ROLE_TEACHER".equals(user.getRole()) || 
                "ROLE_ADMIN".equals(user.getRole()) || 
                "ROLE_SUPER_ADMIN".equals(user.getRole())) {
                // Teachers and admins can see all containers
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
    
    /**
     * Refresh status of a specific container
     */
    @PostMapping("/{id}/refresh-status")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<ContainerInstance> refreshContainerStatus(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            ContainerInstance container = containerInstanceService.findById(id);
            if (container == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if user can access this container
            if ("ROLE_STUDENT".equals(user.getRole()) && !container.getOwner().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            // Refresh the status from Kubernetes
            containerInstanceService.updateContainerStatus(container);
            
            // Return the updated container
            ContainerInstance updated = containerInstanceService.findById(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to refresh container status for container ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Refresh all container statuses
     */
    @PostMapping("/refresh-all-statuses")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshAllContainerStatuses() {
        try {
            List<ContainerInstance> containers = containerInstanceService.getAllContainers();
            int updated = 0;
            
            for (ContainerInstance container : containers) {
                try {
                    containerInstanceService.updateContainerStatus(container);
                    updated++;
                } catch (Exception e) {
                    log.warn("Failed to update status for container {}: {}", container.getName(), e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalContainers", containers.size());
            result.put("updatedContainers", updated);
            result.put("message", "Status refresh completed");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to refresh all container statuses", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Clean up all resources for a specific user (containers, pods, namespace)
     * This is called when a teacher deletes a student to clean up all their resources
     */
    @DeleteMapping("/cleanup-user/{userId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupUserResources(@PathVariable Long userId) {
        try {
            // Get user and their containers
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<ContainerInstance> userContainers = containerInstanceService.getContainersByOwner(user);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("username", user.getUsername());
            result.put("containersDeleted", 0);
            result.put("namespaceCleanup", false);
            result.put("errors", new ArrayList<>());
            
            // Delete all user containers first
            int deletedContainers = 0;
            for (ContainerInstance container : userContainers) {
                try {
                    // For cleanup, use a system user to bypass permission checks
                    User systemUser = new User();
                    systemUser.setRole("ROLE_ADMIN");
                    containerInstanceService.deleteContainer(container.getId(), systemUser);
                    deletedContainers++;
                } catch (Exception e) {
                    log.warn("Failed to delete container {} for user {}: {}", 
                             container.getName(), user.getUsername(), e.getMessage());
                    ((List<String>) result.get("errors")).add("Failed to delete container " + container.getName());
                }
            }
            result.put("containersDeleted", deletedContainers);
            
            // Clean up Kubernetes namespace if user has one
            if (user.getKubernetesNamespace() != null && !user.getKubernetesNamespace().equals("default")) {
                try {
                    namespaceService.deleteStudentNamespace(user.getKubernetesNamespace());
                    result.put("namespaceCleanup", true);
                    result.put("namespaceDeleted", user.getKubernetesNamespace());
                } catch (Exception e) {
                    log.warn("Failed to delete namespace {} for user {}: {}", 
                             user.getKubernetesNamespace(), user.getUsername(), e.getMessage());
                    ((List<String>) result.get("errors")).add("Failed to delete namespace " + user.getKubernetesNamespace());
                }
            }
            
            result.put("message", "Resource cleanup completed for user " + user.getUsername());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to cleanup user resources", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to cleanup user resources: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
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
                // Get real SSH connection details from Kubernetes
                String minikubeIp = containerInstanceService.getMinikubeIp();
                
                // Use student's namespace for SSH port lookup
                String studentNamespace = container.getOwner() != null ? 
                    container.getOwner().getKubernetesNamespace() : "default";
                    
                Integer sshPort = containerInstanceService.getContainerSshPort(
                    container.getKubernetesPodName(), studentNamespace);
                
                sshInfo.put("host", minikubeIp);
                sshInfo.put("port", sshPort);
                sshInfo.put("username", "root");
                sshInfo.put("password", "student123");
                sshInfo.put("dockerImage", "thesis-ssh-container:latest");
                sshInfo.put("ready", true);
                sshInfo.put("namespace", studentNamespace);
                
                // Provide both direct and port-forward instructions
                sshInfo.put("instructions", "Connect using: ssh -p " + sshPort + " root@" + minikubeIp);
                sshInfo.put("note", "This connects to a real SSH-enabled container running in Minikube");
                
                // Add port-forward instructions for better compatibility
                String serviceName = container.getKubernetesPodName() + "-ssh";
                int localPort = 8023; // You can change this to any available port
                
                // Include namespace in port forward command
                String portForwardCmd = studentNamespace.equals("default") ? 
                    "kubectl port-forward service/" + serviceName + " " + localPort + ":22" :
                    "kubectl port-forward -n " + studentNamespace + " service/" + serviceName + " " + localPort + ":22";
                    
                sshInfo.put("portForwardCommand", portForwardCmd);
                sshInfo.put("portForwardSsh", "ssh -p " + localPort + " root@127.0.0.1");
                sshInfo.put("alternativeNote", "If direct connection fails (common on macOS), use port forwarding method below");
                
                // Add port explanations
                sshInfo.put("portExplanation", Map.of(
                    "nodePort", "Port " + sshPort + " is assigned by Kubernetes (NodePort) - this changes with each container",
                    "localPort", "Port " + localPort + " is a local port we choose for convenience - you can use any free port",
                    "why", "Different ports serve different purposes: NodePort for direct access, local port for tunneling"
                ));
                
                // Add step-by-step instructions for better user experience
                sshInfo.put("stepByStepInstructions", Map.of(
                    "step1", "Open a terminal/command prompt",
                    "step2", "Run the port-forward command: kubectl port-forward service/" + serviceName + " " + localPort + ":22",
                    "step3", "Open a new terminal window (keep the first one running)",
                    "step4", "Connect via SSH: ssh -p " + localPort + " root@127.0.0.1",
                    "step5", "Enter password when prompted: student123"
                ));
                
                // Add troubleshooting tips
                sshInfo.put("troubleshooting", Map.of(
                    "connectionRefused", "Make sure the port-forward command is running in a separate terminal",
                    "passwordFailed", "Use password: student123 (case sensitive)",
                    "commandNotFound", "Make sure kubectl is installed and configured for your cluster",
                    "portInUse", "Try a different port like 8024:22 instead of 8023:22"
                ));
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

    /**
     * Start a container
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or @containerInstanceService.canStudentAccessContainer(#id, authentication.name)")
    public ResponseEntity<?> startContainer(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            log.info("User {} starting container {}", user.getUsername(), id);
            ContainerInstance container = containerRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Container not found"));
            
            // Use Kubernetes service to start the pod
            String podName = container.getKubernetesPodName();
            String namespace = container.getOwner() != null ? 
                container.getOwner().getKubernetesNamespace() : "default";
            
            // Start pod logic (this might involve scaling a deployment or patching the pod)
            // For now, just update the status
            container.setStatus("Running");
            containerRepo.save(container);
            
            log.info("Container {} started successfully", id);
            return ResponseEntity.ok(container);
        } catch (Exception e) {
            log.error("Failed to start container {}", id, e);
            return ResponseEntity.badRequest().body("Failed to start container: " + e.getMessage());
        }
    }

    /**
     * Stop a container
     */
    @PostMapping("/{id}/stop")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or @containerInstanceService.canStudentAccessContainer(#id, authentication.name)")
    public ResponseEntity<?> stopContainer(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            log.info("User {} stopping container {}", user.getUsername(), id);
            ContainerInstance container = containerRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Container not found"));
            
            // Stop pod logic
            container.setStatus("Stopped");
            containerRepo.save(container);
            
            log.info("Container {} stopped successfully", id);
            return ResponseEntity.ok(container);
        } catch (Exception e) {
            log.error("Failed to stop container {}", id, e);
            return ResponseEntity.badRequest().body("Failed to stop container: " + e.getMessage());
        }
    }

    /**
     * Delete a container
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or @containerInstanceService.canStudentAccessContainer(#id, authentication.name)")
    public ResponseEntity<?> deleteContainer(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            log.info("User {} deleting container {}", user.getUsername(), id);
            ContainerInstance container = containerRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Container not found"));
            
            // Delete the pod from Kubernetes
            // TODO: Implement deletePod method in KubernetesService
            String podName = container.getKubernetesPodName();
            String namespace = container.getOwner() != null ? 
                container.getOwner().getKubernetesNamespace() : "default";
            
            log.info("Would delete pod {} from namespace {}", podName, namespace);
            
            // Delete from database
            containerRepo.delete(container);
            
            log.info("Container {} deleted successfully", id);
            return ResponseEntity.ok(Map.of("message", "Container deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete container {}", id, e);
            return ResponseEntity.badRequest().body("Failed to delete container: " + e.getMessage());
        }
    }

    /**
     * Get container logs
     */
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getContainerLogs(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            log.info("User {} requesting logs for container {}", user.getUsername(), id);
            String logs = containerInstanceService.getContainerLogs(id, user);
            return ResponseEntity.ok(Map.of("logs", logs));
        } catch (RuntimeException e) {
            log.error("Failed to get logs for container {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get logs for container {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve logs: " + e.getMessage()));
        }
    }
}
