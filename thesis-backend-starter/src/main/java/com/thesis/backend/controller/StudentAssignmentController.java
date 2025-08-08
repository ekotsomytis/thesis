package com.thesis.backend.controller;

import com.thesis.backend.entity.StudentAssignment;
import com.thesis.backend.repository.StudentAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class StudentAssignmentController {

    private final StudentAssignmentRepository assignmentRepository;

    @GetMapping
    public List<StudentAssignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    @GetMapping("/student/{studentId}")
    public List<StudentAssignment> getAssignmentsByStudent(@PathVariable String studentId) {
        return assignmentRepository.findByStudentId(studentId);
    }

    @GetMapping("/student/{studentId}/status/{status}")
    public List<StudentAssignment> getAssignmentsByStudentAndStatus(
            @PathVariable String studentId,
            @PathVariable String status) {
        return assignmentRepository.findByStudentIdAndStatus(studentId, status);
    }

    @GetMapping("/course/{courseId}")
    public List<StudentAssignment> getAssignmentsByCourse(@PathVariable String courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }

    @GetMapping("/course/{courseId}/status/{status}")
    public List<StudentAssignment> getAssignmentsByCourseAndStatus(
            @PathVariable String courseId,
            @PathVariable String status) {
        return assignmentRepository.findByCourseIdAndStatus(courseId, status);
    }

    @GetMapping("/professor/{professorId}")
    public List<StudentAssignment> getAssignmentsByProfessor(@PathVariable String professorId) {
        return assignmentRepository.findByProfessorId(professorId);
    }

    @GetMapping("/template/{imageTemplateId}")
    public List<StudentAssignment> getAssignmentsByImageTemplate(@PathVariable Long imageTemplateId) {
        return assignmentRepository.findByImageTemplateId(imageTemplateId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentAssignment> getAssignmentById(@PathVariable Long id) {
        return assignmentRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public StudentAssignment createAssignment(@RequestBody StudentAssignment assignment) {
        return assignmentRepository.save(assignment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAssignment> updateAssignment(@PathVariable Long id, @RequestBody StudentAssignment assignment) {
        try {
            StudentAssignment existingAssignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
            
            // Create updated assignment using builder
            StudentAssignment updatedAssignment = StudentAssignment.builder()
                .id(existingAssignment.getId())
                .studentId(assignment.getStudentId())
                .courseId(assignment.getCourseId())
                .imageTemplateId(assignment.getImageTemplateId())
                .assignedAt(assignment.getAssignedAt())
                .dueDate(assignment.getDueDate())
                .assignmentDescription(assignment.getAssignmentDescription())
                .status(assignment.getStatus())
                .professorId(assignment.getProfessorId())
                .createdAt(existingAssignment.getCreatedAt())
                .updatedAt(existingAssignment.getUpdatedAt())
                .build();
            
            StudentAssignment savedAssignment = assignmentRepository.save(updatedAssignment);
            return ResponseEntity.ok(savedAssignment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        try {
            assignmentRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/status/{status}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAssignment> updateAssignmentStatus(
            @PathVariable Long id,
            @PathVariable String status) {
        try {
            StudentAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
            
            StudentAssignment updatedAssignment = StudentAssignment.builder()
                .id(assignment.getId())
                .studentId(assignment.getStudentId())
                .courseId(assignment.getCourseId())
                .imageTemplateId(assignment.getImageTemplateId())
                .assignedAt(assignment.getAssignedAt())
                .dueDate(assignment.getDueDate())
                .assignmentDescription(assignment.getAssignmentDescription())
                .status(status)
                .professorId(assignment.getProfessorId())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
            
            StudentAssignment savedAssignment = assignmentRepository.save(updatedAssignment);
            return ResponseEntity.ok(savedAssignment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentAssignment>> createBulkAssignments(
            @RequestBody List<StudentAssignment> assignments) {
        try {
            List<StudentAssignment> savedAssignments = assignmentRepository.saveAll(assignments);
            return ResponseEntity.ok(savedAssignments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 