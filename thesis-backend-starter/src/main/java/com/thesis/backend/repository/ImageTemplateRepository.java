
package com.thesis.backend.repository;

import com.thesis.backend.entity.ImageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageTemplateRepository extends JpaRepository<ImageTemplate, Long> {
    
    List<ImageTemplate> findByTechnology(String technology);
    
    List<ImageTemplate> findByProfessorId(String professorId);
    
    List<ImageTemplate> findByIsPublicTrue();
    
    List<ImageTemplate> findByCourseCode(String courseCode);
    
    @Query("SELECT it FROM ImageTemplate it WHERE it.professorId = :professorId OR it.isPublic = true")
    List<ImageTemplate> findAvailableTemplates(@Param("professorId") String professorId);
    
    @Query("SELECT it FROM ImageTemplate it WHERE it.technology = :technology AND (it.professorId = :professorId OR it.isPublic = true)")
    List<ImageTemplate> findAvailableTemplatesByTechnology(@Param("technology") String technology, @Param("professorId") String professorId);
    
    @Query("SELECT it FROM ImageTemplate it WHERE it.isBuilt = true")
    List<ImageTemplate> findBuiltTemplates();
    
    @Query("SELECT it FROM ImageTemplate it WHERE it.isBuilt = false")
    List<ImageTemplate> findUnbuiltTemplates();
}
