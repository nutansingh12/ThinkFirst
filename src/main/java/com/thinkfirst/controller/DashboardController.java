package com.thinkfirst.controller;

import com.thinkfirst.dto.ProgressReport;
import com.thinkfirst.model.Achievement;
import com.thinkfirst.service.AchievementService;
import com.thinkfirst.service.ProgressTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Parent/Educator dashboard endpoints")
public class DashboardController {
    
    private final ProgressTrackingService progressTrackingService;
    private final AchievementService achievementService;
    
    @GetMapping("/child/{childId}/progress")
    @Operation(summary = "Get comprehensive progress report for a child")
    public ResponseEntity<ProgressReport> getProgressReport(@PathVariable Long childId) {
        return ResponseEntity.ok(progressTrackingService.getProgressReport(childId));
    }
    
    @GetMapping("/child/{childId}/achievements")
    @Operation(summary = "Get all achievements for a child")
    public ResponseEntity<List<Achievement>> getAchievements(@PathVariable Long childId) {
        return ResponseEntity.ok(achievementService.getChildAchievements(childId));
    }
}

