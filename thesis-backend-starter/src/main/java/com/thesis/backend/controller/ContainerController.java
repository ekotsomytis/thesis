
package com.thesis.backend.controller;

import com.thesis.backend.entity.ContainerInstance;
import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.repository.ContainerInstanceRepository;
import com.thesis.backend.repository.ImageTemplateRepository;
import com.thesis.backend.service.KubernetesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final KubernetesService kubeService;
    private final ContainerInstanceRepository containerRepo;
    private final ImageTemplateRepository imageRepo;

    @PostMapping("/create/{imageId}")
    public ResponseEntity<?> create(@PathVariable Long imageId, @RequestParam String username) {
        ImageTemplate template = imageRepo.findById(imageId).orElseThrow();
        String podName = kubeService.createContainer(template.getDockerImage(), username);
        ContainerInstance instance = ContainerInstance.builder()
                .kubernetesPodName(podName)
                .status("Running")
                .name(podName)
                .build();
        return ResponseEntity.ok(containerRepo.save(instance));
    }

    @GetMapping
    public List<ContainerInstance> all() {
        return containerRepo.findAll();
    }
}
