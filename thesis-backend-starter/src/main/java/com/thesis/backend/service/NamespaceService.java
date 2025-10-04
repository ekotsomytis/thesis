package com.thesis.backend.service;

import com.thesis.backend.entity.User;
import com.thesis.backend.model.KubernetesNamespace;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NamespaceService {

    private final KubernetesClient kubernetesClient;
    
    @Value("${kubernetes.namespace.prefix:student-}")
    private String namespacePrefix;
    
    @Value("${kubernetes.rbac.enabled:true}")
    private boolean rbacEnabled;

    /**
     * Get all namespaces
     */
    public List<KubernetesNamespace> getAllNamespaces() {
        return kubernetesClient.namespaces().list().getItems()
                .stream().map(this::mapNamespaceToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get namespace by name
     */
    public KubernetesNamespace getNamespace(String name) {
        Namespace namespace = kubernetesClient.namespaces().withName(name).get();
        if (namespace == null) {
            return null;
        }
        return mapNamespaceToDto(namespace);
    }

    /**
     * Create a new namespace
     */
    public KubernetesNamespace createNamespace(String name, Map<String, String> labels) {
        Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withLabels(labels)
                .endMetadata()
                .build();

        Namespace createdNamespace = kubernetesClient.namespaces().resource(namespace).create();
        return mapNamespaceToDto(createdNamespace);
    }

    /**
     * Delete a namespace
     */
    public boolean deleteNamespace(String name) {
        return !kubernetesClient.namespaces().withName(name).delete().isEmpty();
    }

    /**
     * Map Kubernetes Namespace to a simplified DTO
     */
    private KubernetesNamespace mapNamespaceToDto(Namespace namespace) {
        KubernetesNamespace kubernetesNamespace = new KubernetesNamespace();
        kubernetesNamespace.setName(namespace.getMetadata().getName());
        kubernetesNamespace.setStatus(
                namespace.getStatus() != null ? namespace.getStatus().getPhase() : null);
        
        if (namespace.getMetadata().getLabels() != null) {
            kubernetesNamespace.setLabels(namespace.getMetadata().getLabels());
        }
        
        if (namespace.getMetadata().getCreationTimestamp() != null) {
            kubernetesNamespace.setCreationTimestamp(
                    namespace.getMetadata().getCreationTimestamp());
        }
        
        return kubernetesNamespace;
    }

    /**
     * Create a dedicated namespace for a student with RBAC
     */
    public String createStudentNamespace(User student) {
        String namespaceName = generateNamespaceName(student);
        
        try {
            // Create namespace
            Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                    .withName(namespaceName)
                    .withLabels(Map.of(
                        "type", "student",
                        "student-id", student.getId().toString(),
                        "student-username", student.getUsername(),
                        "managed-by", "thesis-platform"
                    ))
                .endMetadata()
                .build();
            
            kubernetesClient.namespaces().resource(namespace).create();
            log.info("Created namespace: {} for student: {}", namespaceName, student.getUsername());
            
            // Create RBAC if enabled
            if (rbacEnabled) {
                createStudentRBAC(namespaceName, student);
            }
            
            return namespaceName;
        } catch (Exception e) {
            log.error("Failed to create namespace for student: {}", student.getUsername(), e);
            throw new RuntimeException("Failed to create student namespace", e);
        }
    }
    
    /**
     * Create RBAC rules for student in their namespace
     */
    private void createStudentRBAC(String namespaceName, User student) {
        try {
            String roleName = "student-" + student.getUsername();
            String roleBindingName = "student-" + student.getUsername() + "-binding";
            String serviceAccountName = "student-" + student.getUsername();
            
            // Create ServiceAccount
            ServiceAccount serviceAccount = new ServiceAccountBuilder()
                .withNewMetadata()
                    .withName(serviceAccountName)
                    .withNamespace(namespaceName)
                .endMetadata()
                .build();
            
            kubernetesClient.serviceAccounts()
                .inNamespace(namespaceName)
                .resource(serviceAccount)
                .create();
            
            // Create Role with student permissions (full control within their namespace)
            Role role = new RoleBuilder()
                .withNewMetadata()
                    .withName(roleName)
                    .withNamespace(namespaceName)
                    .addToLabels("managed-by", "thesis-platform")
                    .addToLabels("student", student.getUsername())
                .endMetadata()
                .withRules(
                    // Allow full pod management (create, read, update, delete, exec)
                    new PolicyRuleBuilder()
                        .withApiGroups("")
                        .withResources("pods", "pods/log", "pods/status", "pods/exec", "pods/portforward")
                        .withVerbs("get", "list", "create", "delete", "watch", "update", "patch")
                        .build(),
                    // Allow service management
                    new PolicyRuleBuilder()
                        .withApiGroups("")
                        .withResources("services", "endpoints")
                        .withVerbs("get", "list", "create", "delete", "update", "patch")
                        .build(),
                    // Allow configmap access (for configuration)
                    new PolicyRuleBuilder()
                        .withApiGroups("")
                        .withResources("configmaps")
                        .withVerbs("get", "list", "create", "update", "delete", "patch")
                        .build(),
                    // Allow secret access (for credentials)
                    new PolicyRuleBuilder()
                        .withApiGroups("")
                        .withResources("secrets")
                        .withVerbs("get", "list", "create", "delete")
                        .build(),
                    // Allow persistent volume claim management
                    new PolicyRuleBuilder()
                        .withApiGroups("")
                        .withResources("persistentvolumeclaims")
                        .withVerbs("get", "list", "create", "delete", "update")
                        .build(),
                    // Allow deployment management (if using deployments)
                    new PolicyRuleBuilder()
                        .withApiGroups("apps")
                        .withResources("deployments", "replicasets")
                        .withVerbs("get", "list", "create", "delete", "update", "patch")
                        .build(),
                    // Allow resource quota viewing
                    new PolicyRuleBuilder()
                        .withApiGroups("")
                        .withResources("resourcequotas")
                        .withVerbs("get", "list")
                        .build(),
                    // Allow viewing events
                    new PolicyRuleBuilder()
                        .withApiGroups("")
                        .withResources("events")
                        .withVerbs("get", "list", "watch")
                        .build()
                )
                .build();
            
            kubernetesClient.rbac().roles()
                .inNamespace(namespaceName)
                .resource(role)
                .create();
            
            // Create RoleBinding
            RoleBinding roleBinding = new RoleBindingBuilder()
                .withNewMetadata()
                    .withName(roleBindingName)
                    .withNamespace(namespaceName)
                .endMetadata()
                .withSubjects(
                    new SubjectBuilder()
                        .withKind("ServiceAccount")
                        .withName(serviceAccountName)
                        .withNamespace(namespaceName)
                        .build()
                )
                .withRoleRef(
                    new RoleRefBuilder()
                        .withKind("Role")
                        .withName(roleName)
                        .withApiGroup("rbac.authorization.k8s.io")
                        .build()
                )
                .build();
            
            kubernetesClient.rbac().roleBindings()
                .inNamespace(namespaceName)
                .resource(roleBinding)
                .create();
            
            log.info("Created RBAC for student: {} in namespace: {}", student.getUsername(), namespaceName);
            
            // Create ResourceQuota to limit resource usage
            createResourceQuota(namespaceName, student);
            
            // Create NetworkPolicy for isolation (optional, may not work in all clusters)
            createNetworkPolicy(namespaceName, student);
            
        } catch (Exception e) {
            log.error("Failed to create RBAC for student: {} in namespace: {}", student.getUsername(), namespaceName, e);
            throw new RuntimeException("Failed to create student RBAC", e);
        }
    }
    
    /**
     * Create ResourceQuota to limit student resource usage
     */
    private void createResourceQuota(String namespaceName, User student) {
        try {
            ResourceQuota quota = new ResourceQuotaBuilder()
                .withNewMetadata()
                    .withName("student-quota")
                    .withNamespace(namespaceName)
                    .addToLabels("managed-by", "thesis-platform")
                    .addToLabels("student", student.getUsername())
                .endMetadata()
                .withNewSpec()
                    .addToHard("pods", new Quantity("10"))                    // Max 10 pods
                    .addToHard("requests.cpu", new Quantity("4"))             // Max 4 CPU cores
                    .addToHard("requests.memory", new Quantity("8Gi"))        // Max 8GB RAM
                    .addToHard("limits.cpu", new Quantity("8"))               // Max 8 CPU limit
                    .addToHard("limits.memory", new Quantity("16Gi"))         // Max 16GB RAM limit
                    .addToHard("persistentvolumeclaims", new Quantity("5"))   // Max 5 PVCs
                    .addToHard("requests.storage", new Quantity("20Gi"))      // Max 20GB storage
                    .addToHard("services", new Quantity("10"))                // Max 10 services
                    .addToHard("configmaps", new Quantity("20"))              // Max 20 configmaps
                    .addToHard("secrets", new Quantity("20"))                 // Max 20 secrets
                .endSpec()
                .build();
            
            kubernetesClient.resourceQuotas()
                .inNamespace(namespaceName)
                .resource(quota)
                .create();
            
            log.info("Created ResourceQuota for student: {} in namespace: {}", student.getUsername(), namespaceName);
            
        } catch (Exception e) {
            log.warn("Failed to create ResourceQuota for student: {} - continuing anyway", student.getUsername(), e);
            // Don't fail if quota creation fails - namespace and RBAC are more important
        }
    }
    
    /**
     * Create NetworkPolicy to isolate student namespace
     */
    private void createNetworkPolicy(String namespaceName, User student) {
        try {
            io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy networkPolicy = 
                new io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder()
                .withNewMetadata()
                    .withName("student-isolation")
                    .withNamespace(namespaceName)
                    .addToLabels("managed-by", "thesis-platform")
                    .addToLabels("student", student.getUsername())
                .endMetadata()
                .withNewSpec()
                    .withNewPodSelector() // Apply to all pods in namespace
                    .endPodSelector()
                    .addNewIngress()
                        .addNewFrom()
                            .withNewNamespaceSelector()
                                .addToMatchLabels("name", namespaceName) // Only from same namespace
                            .endNamespaceSelector()
                        .endFrom()
                    .endIngress()
                    .addNewEgress()
                        // Allow all egress (internet access)
                    .endEgress()
                    .withPolicyTypes("Ingress", "Egress")
                .endSpec()
                .build();
            
            kubernetesClient.network().networkPolicies()
                .inNamespace(namespaceName)
                .resource(networkPolicy)
                .create();
            
            log.info("Created NetworkPolicy for student: {} in namespace: {}", student.getUsername(), namespaceName);
            
        } catch (Exception e) {
            log.warn("Failed to create NetworkPolicy for student: {} - continuing anyway", student.getUsername(), e);
            // Don't fail if network policy creation fails
        }
    }
    
    /**
     * Delete student namespace and all resources
     */
    public void deleteStudentNamespace(User student) {
        String namespaceName = generateNamespaceName(student);
        
        try {
            kubernetesClient.namespaces().withName(namespaceName).delete();
            log.info("Deleted namespace: {} for student: {}", namespaceName, student.getUsername());
        } catch (Exception e) {
            log.error("Failed to delete namespace for student: {}", student.getUsername(), e);
            throw new RuntimeException("Failed to delete student namespace", e);
        }
    }
    
    /**
     * Check if student namespace exists
     */
    public boolean studentNamespaceExists(User student) {
        String namespaceName = generateNamespaceName(student);
        return kubernetesClient.namespaces().withName(namespaceName).get() != null;
    }
    
    /**
     * Get or create student namespace
     */
    public String getOrCreateStudentNamespace(User student) {
        String namespaceName = generateNamespaceName(student);
        
        if (!studentNamespaceExists(student)) {
            return createStudentNamespace(student);
        }
        
        return namespaceName;
    }
    
    /**
     * Generate namespace name for student
     */
    private String generateNamespaceName(User student) {
        return namespacePrefix + student.getUsername().toLowerCase().replaceAll("[^a-z0-9-]", "");
    }
    
    /**
     * Clean up all resources in student namespace
     */
    public void cleanupStudentResources(User student) {
        String namespaceName = generateNamespaceName(student);
        
        try {
            // Delete all pods
            kubernetesClient.pods().inNamespace(namespaceName).delete();
            
            // Delete all services
            kubernetesClient.services().inNamespace(namespaceName).delete();
            
            // Delete all configmaps
            kubernetesClient.configMaps().inNamespace(namespaceName).delete();
            
            log.info("Cleaned up resources in namespace: {} for student: {}", namespaceName, student.getUsername());
        } catch (Exception e) {
            log.error("Failed to cleanup resources for student: {}", student.getUsername(), e);
        }
    }
    
    /**
     * Delete a student namespace and all its resources
     */
    public void deleteStudentNamespace(String namespaceName) {
        try {
            if ("default".equals(namespaceName)) {
                log.warn("Cannot delete default namespace");
                return;
            }
            
            log.info("Deleting namespace: {}", namespaceName);
            
            // Delete the namespace (this will cascade delete all resources in it)
            kubernetesClient.namespaces()
                    .withName(namespaceName)
                    .delete();
            
            log.info("Successfully deleted namespace: {}", namespaceName);
            
        } catch (Exception e) {
            log.error("Failed to delete namespace: {}", namespaceName, e);
            throw new RuntimeException("Failed to delete namespace: " + namespaceName, e);
        }
    }
    
    /**
     * Check if a namespace exists
     */
    public boolean namespaceExists(String namespaceName) {
        try {
            return kubernetesClient.namespaces()
                    .withName(namespaceName)
                    .get() != null;
        } catch (Exception e) {
            log.error("Failed to check namespace existence: {}", namespaceName, e);
            return false;
        }
    }
}
