
package com.thesis.backend.controller;

import com.thesis.backend.entity.ImageTemplate;
import com.thesis.backend.repository.ImageTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageTemplateController {

    private final ImageTemplateRepository repo;

    @GetMapping
    public List<ImageTemplate> all() {
        return repo.findAll();
    }

    @PostMapping
    public ImageTemplate create(@RequestBody ImageTemplate template) {
        return repo.save(template);
    }
}
