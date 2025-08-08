package com.thesis.backend.repository;

import com.thesis.backend.entity.StudentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAssignmentRepository extends JpaRepository<StudentAssignment, Long> {
    
    List<StudentAssignment> findByStudentId(String studentId);
    
    List<StudentAssignment> findByCourseId(String courseId);
    
    List<StudentAssignment> findByProfessorId(String professorId);
    
    @Query("SELECT sa FROM StudentAssignment sa WHERE sa.studentId = :studentId AND sa.status = :status")
    List<StudentAssignment> findByStudentIdAndStatus(@Param("studentId") String studentId, @Param("status") String status);
    
    @Query("SELECT sa FROM StudentAssignment sa WHERE sa.courseId = :courseId AND sa.status = :status")
    List<StudentAssignment> findByCourseIdAndStatus(@Param("courseId") String courseId, @Param("status") String status);
    
    @Query("SELECT sa FROM StudentAssignment sa WHERE sa.imageTemplateId = :imageTemplateId")
    List<StudentAssignment> findByImageTemplateId(@Param("imageTemplateId") Long imageTemplateId);
} 