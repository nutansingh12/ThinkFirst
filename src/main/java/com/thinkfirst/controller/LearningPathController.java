package com.thinkfirst.controller;

import com.thinkfirst.dto.LearningPathResponse;
import com.thinkfirst.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/learning-path")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @PostMapping("/lesson/{lessonId}/complete")
    public ResponseEntity<LearningPathResponse> completeLesson(
            @PathVariable Long lessonId,
            @RequestParam Long childId
    ) {
        log.info("Completing lesson {} for child {}", lessonId, childId);

        try {
            LearningPathResponse response = learningPathService.completeLesson(childId, lessonId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to complete lesson {} for child {}: {}", lessonId, childId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{learningPathId}")
    public ResponseEntity<LearningPathResponse> getLearningPath(
            @PathVariable Long learningPathId,
            @RequestParam Long childId
    ) {
        log.info("Getting learning path {} for child {}", learningPathId, childId);

        try {
            LearningPathResponse response = learningPathService.getLearningPath(childId, learningPathId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get learning path {} for child {}: {}", learningPathId, childId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}

