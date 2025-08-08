
package com.thesis.backend.controller;

import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.service.DockerImageBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageTemplateController {

    private final ImageTemplateRepository repo;
    private final DockerImageBuilderService dockerImageBuilderService;

    @GetMapping("/test")
    public String test() {
        System.out.println("DEBUG: Test endpoint called");
        return "Test endpoint works!";
    }

    @GetMapping
    public List<ImageTemplate> all() {
        System.out.println("DEBUG: ImageTemplateController.all() called");
        try {
            List<ImageTemplate> result = repo.findAll();
            System.out.println("DEBUG: Found " + result.size() + " templates");
            return result;
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in all(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/available")
    public List<ImageTemplate> getAvailableTemplates(@RequestParam String professorId) {
        return repo.findAvailableTemplates(professorId);
    }

    @GetMapping("/technology/{technology}")
    public List<ImageTemplate> getTemplatesByTechnology(
            @PathVariable String technology,
            @RequestParam String professorId) {
        return repo.findAvailableTemplatesByTechnology(technology, professorId);
    }

    @GetMapping("/built")
    public List<ImageTemplate> getBuiltTemplates() {
        return repo.findBuiltTemplates();
    }

    @GetMapping("/unbuilt")
    public List<ImageTemplate> getUnbuiltTemplates() {
        return repo.findUnbuiltTemplates();
    }

    @GetMapping("/course/{courseCode}")
    public List<ImageTemplate> getTemplatesByCourse(@PathVariable String courseCode) {
        return repo.findByCourseCode(courseCode);
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ImageTemplate create(@RequestBody ImageTemplate template) {
        return repo.save(template);
    }

    @PostMapping("/{id}/build")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ImageTemplate> buildImage(@PathVariable Long id) {
        try {
            ImageTemplate template = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
            
            ImageTemplate updatedTemplate = dockerImageBuilderService.buildImageAndUpdateTemplate(template);
            ImageTemplate savedTemplate = repo.save(updatedTemplate);
            
            return ResponseEntity.ok(savedTemplate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ImageTemplate> update(@PathVariable Long id, @RequestBody ImageTemplate template) {
        try {
            ImageTemplate existingTemplate = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
            
            // Create updated template using builder
            ImageTemplate updatedTemplate = ImageTemplate.builder()
                .id(existingTemplate.getId())
                .name(template.getName())
                .dockerImage(template.getDockerImage())
                .description(template.getDescription())
                .technology(template.getTechnology())
                .version(template.getVersion())
                .dockerfile(template.getDockerfile())
                .preInstalledTools(template.getPreInstalledTools())
                .environmentVariables(template.getEnvironmentVariables())
                .startupCommands(template.getStartupCommands())
                .courseCode(template.getCourseCode())
                .professorId(template.getProfessorId())
                .isPublic(template.isPublic())
                .isBuilt(template.isBuilt())
                .createdAt(existingTemplate.getCreatedAt())
                .updatedAt(existingTemplate.getUpdatedAt())
                .build();
            
            ImageTemplate savedTemplate = repo.save(updatedTemplate);
            return ResponseEntity.ok(savedTemplate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            repo.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/technologies")
    public List<String> getAvailableTechnologies() {
        return dockerImageBuilderService.getAvailableTechnologies();
    }

    @GetMapping("/technology-versions")
    public Map<String, List<String>> getTechnologyVersions() {
        return dockerImageBuilderService.getTechnologyVersions();
    }
}
