package com.example.kinopoiskservice.controllers;

import com.example.kinopoiskservice.services.ContentDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content-details")
public class ContentDetailsController {

    private final ContentDetailsService contentDetailsService;

    public ContentDetailsController(ContentDetailsService contentDetailsService) {
        this.contentDetailsService = contentDetailsService;
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<?> getContentDetails(
            @PathVariable Long contentId,
            @RequestParam(required = false) Long userId) {

        var details = contentDetailsService.getContentDetails(contentId, userId);

        if (details == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(details);
    }
}