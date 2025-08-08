package com.thesis.backend.repository;

import com.thesis.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    List<Course> findByProfessorId(String professorId);
    
    List<Course> findByIsActiveTrue();
    
    @Query("SELECT c FROM Course c WHERE c.professorId = :professorId AND c.isActive = true")
    List<Course> findActiveCoursesByProfessor(@Param("professorId") String professorId);
    
    Course findByCode(String code);
    
    @Query("SELECT c FROM Course c WHERE c.code = :code AND c.isActive = true")
    Course findActiveCourseByCode(@Param("code") String code);
} 