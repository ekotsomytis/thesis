package com.thesis.backend.service;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ContainerTemplate;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ContainerTemplateRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.repository.UserRepository;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerInstanceService {
    
    private final ContainerInstanceRepository containerInstanceRepository;
    private final ContainerTemplateRepository containerTemplateRepository;
    private final ImageTemplateRepository imageTemplateRepository;
    private final UserRepository userRepository;
    private final KubernetesClient kubernetesClient;
    
    @Value("${ssh.container.namespace:default}")
    private String namespace;
    
    /**
     * Create a container instance from a template for a student
     */
    public ContainerInstance createContainerFromTemplate(Long templateId, User student, User teacher) {
        ContainerTemplate template = containerTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Container template not found"));
        
        // Generate unique name for the container
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String containerName = String.format("%s-%s-%s", 
                template.getName().toLowerCase().replaceAll("[^a-z0-9]", "-"),
                student.getUsername().toLowerCase(),
                timestamp);
        
        // Create Kubernetes pod from template
        String podName = createKubernetesPod(template, containerName, student);
        
        // Create container instance record
        ContainerInstance instance = ContainerInstance.builder()
                .name(containerName)
                .status("Creating")
                .kubernetesPodName(podName)
                .owner(student)
                .imageTemplate(null) // We'll set this based on template if needed
                .build();
        
        ContainerInstance savedInstance = containerInstanceRepository.save(instance);
        
        // Update status after pod is created
        updateContainerStatus(savedInstance);
        
        log.info("Created container instance {} for student {} from template {}", 
                containerName, student.getUsername(), template.getName());
        
        return savedInstance;
    }
    
    /**
     * Create a container instance from an image template for a student (by IDs)
     */
    public ContainerInstance createContainerForStudent(Long imageId, Long studentId, User teacher) {
        // Find the student
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        // Verify the user is actually a student
        if (!"ROLE_STUDENT".equals(student.getRole())) {
            throw new RuntimeException("User is not a student");
        }
        
        // For now, we'll use the existing simple container creation
        // TODO: Integrate with the more complex template system later
        return createSimpleContainerForStudent(imageId, student, teacher);
    }

    private ContainerInstance createSimpleContainerForStudent(Long imageId, User student, User teacher) {
        // Find the image template
        ImageTemplate imageTemplate = imageTemplateRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image template not found with id: " + imageId));
        
        // Generate unique name for the container
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String containerName = String.format("container-%s-%s", 
                student.getUsername().toLowerCase(),
                timestamp);
        
        // Create Kubernetes pod
        String podName = createSimpleKubernetesPod(imageTemplate, containerName, student);
        
        // Create container instance record
        ContainerInstance instance = ContainerInstance.builder()
                .name(containerName)
                .status("Creating")
                .kubernetesPodName(podName)
                .owner(student)
                .imageTemplate(imageTemplate) // Set the ImageTemplate
                .build();
        
        ContainerInstance savedInstance = containerInstanceRepository.save(instance);
        
        // Update status after pod creation
        updateContainerStatus(savedInstance);
        
        log.info("Created container instance {} for student {} using image template {} by teacher {}", 
                containerName, student.getUsername(), imageTemplate.getName(), teacher.getUsername());
        
        return savedInstance;
    }

    /**
     * Get all containers for a student
     */
    public List<ContainerInstance> getStudentContainers(User student) {
        return containerInstanceRepository.findByOwner(student);
    }
    
    /**
     * Get all containers (for teachers)
     */
    public List<ContainerInstance> getAllContainers() {
        return containerInstanceRepository.findAll();
    }
    
    /**
     * Stop a container
     */
    public void stopContainer(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        
        // Check authorization
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete the Kubernetes pod
        kubernetesClient.pods().inNamespace(namespace).withName(instance.getKubernetesPodName()).delete();
        
        // Update status
        instance.setStatus("Stopped");
        containerInstanceRepository.save(instance);
        
        log.info("Stopped container {} by user {}", instance.getName(), user.getUsername());
    }
    
    /**
     * Start a stopped container
     */
    public void startContainer(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // For now, we'll recreate the pod since Kubernetes doesn't support start/stop
        // In a production environment, you might want to use deployments instead
        instance.setStatus("Starting");
        containerInstanceRepository.save(instance);
        
        log.info("Started container {} by user {}", instance.getName(), user.getUsername());
    }
    
    /**
     * Delete a container permanently
     */
    public void deleteContainer(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete Kubernetes resources
        kubernetesClient.pods().inNamespace(namespace).withName(instance.getKubernetesPodName()).delete();
        
        // Delete from database
        containerInstanceRepository.delete(instance);
        
        log.info("Deleted container {} by user {}", instance.getName(), user.getUsername());
    }
    
    /**
     * Get container logs
     */
    public String getContainerLogs(Long instanceId, User user) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        
        if (!canAccessContainer(instance, user)) {
            throw new RuntimeException("Access denied");
        }
        
        try {
            return kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(instance.getKubernetesPodName())
                    .getLog();
        } catch (Exception e) {
            log.error("Failed to get logs for container {}: {}", instance.getName(), e.getMessage());
            return "Failed to retrieve logs: " + e.getMessage();
        }
    }
    
    /**
     * Create Kubernetes pod from template
     */
    private String createKubernetesPod(ContainerTemplate template, String containerName, User student) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", containerName);
        labels.put("owner", student.getUsername());
        labels.put("type", "student-container");
        labels.put("ssh-enabled", template.getSshEnabled().toString());
        
        // Start building container
        ContainerBuilder containerBuilder = new ContainerBuilder()
                .withName("main-container");
        
        // Set image based on SSH requirements
        if (template.getSshEnabled()) {
            containerBuilder = containerBuilder
                    .withImage("thesis-ssh-container:latest")
                    .addNewPort()
                        .withContainerPort(22)
                        .withProtocol("TCP")
                    .endPort()
                    .addNewEnv()
                        .withName("ROOT_PASSWORD")
                        .withValue("rootpass123")
                    .endEnv();
        } else {
            containerBuilder = containerBuilder.withImage(template.getDockerImage());
        }
        
        // Add environment variables from template
        if (template.getEnvironmentVars() != null && !template.getEnvironmentVars().isEmpty()) {
            containerBuilder = containerBuilder
                    .addNewEnv()
                        .withName("WORKSPACE_USER")
                        .withValue(student.getUsername())
                    .endEnv();
        }
        
        // Add resource limits if specified
        if (template.getResourceLimits() != null && !template.getResourceLimits().isEmpty()) {
            containerBuilder = containerBuilder
                    .withNewResources()
                        .addToRequests("memory", new Quantity("256Mi"))
                        .addToRequests("cpu", new Quantity("100m"))
                        .addToLimits("memory", new Quantity("512Mi"))
                        .addToLimits("cpu", new Quantity("500m"))
                    .endResources();
        }
        
        // Add persistent volume mount if required
        if (template.getPersistentStorage()) {
            containerBuilder = containerBuilder
                    .addNewVolumeMount()
                        .withName("workspace-storage")
                        .withMountPath("/workspace")
                    .endVolumeMount();
        }
        
        // Build the pod spec
        PodSpecBuilder podSpecBuilder = new PodSpecBuilder()
                .addToContainers(containerBuilder.build());
        
        // Add persistent volume if required
        if (template.getPersistentStorage()) {
            podSpecBuilder = podSpecBuilder
                .addNewVolume()
                    .withName("workspace-storage")
                    .withNewPersistentVolumeClaim()
                        .withClaimName(containerName + "-pvc")
                    .endPersistentVolumeClaim()
                .endVolume();
        }
        
        // Build the complete pod
        Pod pod = new PodBuilder()
                .withNewMetadata()
                    .withName(containerName)
                    .withNamespace(namespace)
                    .withLabels(labels)
                .endMetadata()
                .withSpec(podSpecBuilder.build())
                .build();
        
        // Create PVC if persistent storage is required
        if (template.getPersistentStorage()) {
            createPersistentVolumeClaim(containerName, template.getStorageSize());
        }
        
        // Create the pod
        kubernetesClient.pods().inNamespace(namespace).create(pod);        
        return containerName;
    }

    /**
     * Create PVC for persistent storage
     */
    private void createPersistentVolumeClaim(String name, String size) {
        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                    .withName(name + "-pvc")
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withAccessModes("ReadWriteOnce")
                    .withNewResources()
                        .addToRequests("storage", new Quantity(size != null ? size : "1Gi"))
                    .endResources()
                .endSpec()
                .build();
        
        kubernetesClient.persistentVolumeClaims().inNamespace(namespace).create(pvc);
    }
    
    /**
     * Update container status by checking Kubernetes pod status
     */
    public void updateContainerStatus(ContainerInstance instance) {
        try {
            Pod pod = kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(instance.getKubernetesPodName())
                    .get();
            
            if (pod != null && pod.getStatus() != null) {
                String phase = pod.getStatus().getPhase();
                instance.setStatus(phase);
                containerInstanceRepository.save(instance);
                log.info("Updated container {} status to {}", instance.getName(), phase);
            }
        } catch (Exception e) {
            log.warn("Could not get pod status from Kubernetes (development mode): {}", e.getMessage());
            
            // In development mode, simulate status progression
            if ("Creating".equals(instance.getStatus())) {
                // Simulate containers becoming "Running" after a short time
                instance.setStatus("Running");
                containerInstanceRepository.save(instance);
                log.info("Simulated container {} status updated to Running", instance.getName());
            }
        }
    }
    
    /**
     * Check if user can access the container
     */
    private boolean canAccessContainer(ContainerInstance instance, User user) {
        // Students can only access their own containers
        if ("ROLE_STUDENT".equals(user.getRole())) {
            return instance.getOwner().getId().equals(user.getId());
        }
        
        // Teachers can access all containers
        return "ROLE_TEACHER".equals(user.getRole());
    }
    
    /**
     * Create a simple Kubernetes pod for student containers with SSH access
     * In development mode, this will simulate pod creation
     */
    private String createSimpleKubernetesPod(ImageTemplate imageTemplate, String containerName, User student) {
        try {
            Map<String, String> labels = new HashMap<>();
            labels.put("app", containerName);
            labels.put("owner", student.getUsername());
            labels.put("type", "student-container");
            labels.put("ssh-enabled", "true");
            
            // Create a simple container with SSH enabled
            Container container = new ContainerBuilder()
                    .withName("main-container")
                    .withImage(imageTemplate.getDockerImage())
                    .addNewPort()
                        .withContainerPort(22)
                        .withProtocol("TCP")
                    .endPort()
                    .addNewEnv()
                        .withName("ROOT_PASSWORD")
                        .withValue("student123") // Simple password for educational purposes
                    .endEnv()
                    .addNewEnv()
                        .withName("SSH_ENABLED")
                        .withValue("true")
                    .endEnv()
                    .addNewEnv()
                        .withName("WORKSPACE_USER")
                        .withValue(student.getUsername())
                    .endEnv()
                    .withNewResources()
                        .addToRequests("memory", new Quantity("256Mi"))
                        .addToRequests("cpu", new Quantity("100m"))
                        .addToLimits("memory", new Quantity("512Mi"))
                        .addToLimits("cpu", new Quantity("500m"))
                    .endResources()
                    .build();
            
            // Build the pod
            Pod pod = new PodBuilder()
                    .withNewMetadata()
                        .withName(containerName)
                        .withNamespace(namespace)
                        .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                        .addToContainers(container)
                        .withRestartPolicy("Always")
                    .endSpec()
                    .build();
            
            // Try to create the pod, but catch any connection errors for development
            kubernetesClient.pods().inNamespace(namespace).create(pod);
            
            log.info("Created Kubernetes pod {} for student {} with image {}", 
                    containerName, student.getUsername(), imageTemplate.getDockerImage());
                    
        } catch (Exception e) {
            // In development mode, log the error but continue (simulate pod creation)
            log.warn("Could not create actual Kubernetes pod (development mode): {}", e.getMessage());
            log.info("Simulating pod creation for {} - student {} with image {}", 
                    containerName, student.getUsername(), imageTemplate.getDockerImage());
        }
        
        return containerName;
    }
    
    /**
     * Check if a student can access a specific container by username
     */
    public boolean canStudentAccessContainer(Long containerId, String username) {
        try {
            ContainerInstance container = containerInstanceRepository.findById(containerId)
                    .orElse(null);
            
            if (container == null) {
                return false;
            }
            
            return container.getOwner().getUsername().equals(username);
        } catch (Exception e) {
            log.error("Error checking container access for user {}: {}", username, e.getMessage());
            return false;
        }
    }
}
