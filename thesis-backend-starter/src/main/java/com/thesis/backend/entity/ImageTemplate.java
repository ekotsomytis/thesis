
package com.thesis.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "image_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ImageTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "docker_image")
    private String dockerImage;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "technology")
    private String technology; // e.g., "python", "nodejs", "java", "golang"
    
    @Column(name = "version")
    private String version; // e.g., "3.9", "16", "11", "1.21"
    
    @Column(name = "dockerfile", columnDefinition = "TEXT")
    private String dockerfile; // Custom Dockerfile content
    
    @Column(name = "pre_installed_tools", columnDefinition = "TEXT")
    private String preInstalledTools; // JSON array of tools
    
    @Column(name = "environment_variables", columnDefinition = "TEXT")
    private String environmentVariables; // JSON object
    
    @Column(name = "startup_commands", columnDefinition = "TEXT")
    private String startupCommands; // Commands to run on container start
    
    @Column(name = "course_code")
    private String courseCode; // Associated course
    
    @Column(name = "professor_id")
    private String professorId; // Who created it
    
    @Column(name = "is_public")
    @Builder.Default
    private boolean isPublic = false; // Can other professors use it
    
    @Column(name = "is_built")
    @Builder.Default
    private boolean isBuilt = false; // Whether the image has been built
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
