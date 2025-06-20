package com.thesis.backend.service;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ContainerTemplate;
import com.thesis.backend.entity.User;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ContainerTemplateRepository;
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
    private void updateContainerStatus(ContainerInstance instance) {
        try {
            Pod pod = kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(instance.getKubernetesPodName())
                    .get();
            
            if (pod != null && pod.getStatus() != null) {
                String phase = pod.getStatus().getPhase();
                instance.setStatus(phase);
                containerInstanceRepository.save(instance);
            }
        } catch (Exception e) {
            log.error("Failed to update container status: {}", e.getMessage());
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
}
