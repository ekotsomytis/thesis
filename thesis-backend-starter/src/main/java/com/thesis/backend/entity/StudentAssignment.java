package com.thesis.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class StudentAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private String studentId;
    
    @Column(name = "course_id", nullable = false)
    private String courseId;
    
    @Column(name = "image_template_id", nullable = false)
    private Long imageTemplateId;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "assignment_description", columnDefinition = "TEXT")
    private String assignmentDescription;
    
    @Column(name = "status")
    @Builder.Default
    private String status = "ASSIGNED"; // ASSIGNED, IN_PROGRESS, COMPLETED
    
    @Column(name = "professor_id")
    private String professorId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 