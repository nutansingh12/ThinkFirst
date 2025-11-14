package com.thinkfirst.controller;

import com.thinkfirst.dto.BadgeDTO;
import com.thinkfirst.dto.LearningProfileDTO;
import com.thinkfirst.service.BadgeService;
import com.thinkfirst.service.SubjectStatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for student learning profile and achievements
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    
    private final SubjectStatisticsService subjectStatisticsService;
    private final BadgeService badgeService;
    
    public ProfileController(
            SubjectStatisticsService subjectStatisticsService,
            BadgeService badgeService
    ) {
        this.subjectStatisticsService = subjectStatisticsService;
        this.badgeService = badgeService;
    }
    
    /**
     * Get learning profile for a child
     */
    @GetMapping("/{childId}")
    public ResponseEntity<LearningProfileDTO> getLearningProfile(@PathVariable Long childId) {
        LearningProfileDTO profile = subjectStatisticsService.getLearningProfile(childId);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * Get all badges for a child (earned and unearned)
     */
    @GetMapping("/{childId}/badges")
    public ResponseEntity<List<BadgeDTO>> getBadges(@PathVariable Long childId) {
        List<BadgeDTO> badges = badgeService.getChildBadges(childId);
        return ResponseEntity.ok(badges);
    }
    
    /**
     * Get newly earned badges that haven't been seen
     */
    @GetMapping("/{childId}/badges/new")
    public ResponseEntity<List<BadgeDTO>> getNewBadges(@PathVariable Long childId) {
        List<BadgeDTO> newBadges = badgeService.getUnseenBadges(childId);
        return ResponseEntity.ok(newBadges);
    }
    
    /**
     * Mark badge notifications as seen
     */
    @PostMapping("/{childId}/badges/mark-seen")
    public ResponseEntity<Void> markBadgesAsSeen(@PathVariable Long childId) {
        badgeService.markBadgesAsSeen(childId);
        return ResponseEntity.ok().build();
    }
}

