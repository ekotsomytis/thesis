package com.thesis.backend.controller;

import com.thesis.backend.entity.Course;
import com.thesis.backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseRepository courseRepository;

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @GetMapping("/active")
    public List<Course> getActiveCourses() {
        return courseRepository.findByIsActiveTrue();
    }

    @GetMapping("/professor/{professorId}")
    public List<Course> getCoursesByProfessor(@PathVariable String professorId) {
        return courseRepository.findActiveCoursesByProfessor(professorId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return courseRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Course> getCourseByCode(@PathVariable String code) {
        Course course = courseRepository.findActiveCourseByCode(code);
        if (course != null) {
            return ResponseEntity.ok(course);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Course createCourse(@RequestBody Course course) {
        return courseRepository.save(course);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        try {
            Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            // Create updated course using builder
            Course updatedCourse = Course.builder()
                .id(existingCourse.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .professorId(course.getProfessorId())
                .academicYear(course.getAcademicYear())
                .semester(course.getSemester())
                .isActive(course.isActive())
                .createdAt(existingCourse.getCreatedAt())
                .updatedAt(existingCourse.getUpdatedAt())
                .build();
            
            Course savedCourse = courseRepository.save(updatedCourse);
            return ResponseEntity.ok(savedCourse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        try {
            courseRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Course> activateCourse(@PathVariable Long id) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            Course activatedCourse = Course.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .professorId(course.getProfessorId())
                .academicYear(course.getAcademicYear())
                .semester(course.getSemester())
                .isActive(true)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
            
            Course savedCourse = courseRepository.save(activatedCourse);
            return ResponseEntity.ok(savedCourse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Course> deactivateCourse(@PathVariable Long id) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            Course deactivatedCourse = Course.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .professorId(course.getProfessorId())
                .academicYear(course.getAcademicYear())
                .semester(course.getSemester())
                .isActive(false)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
            
            Course savedCourse = courseRepository.save(deactivatedCourse);
            return ResponseEntity.ok(savedCourse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 