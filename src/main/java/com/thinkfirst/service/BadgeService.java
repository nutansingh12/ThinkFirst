package com.thinkfirst.service;

import com.thinkfirst.dto.BadgeDTO;
import com.thinkfirst.model.*;
import com.thinkfirst.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing achievement badges
 */
@Service
public class BadgeService {
    
    private static final Logger log = LoggerFactory.getLogger(BadgeService.class);
    
    private final BadgeRepository badgeRepository;
    private final ChildBadgeRepository childBadgeRepository;
    private final SubjectStatisticsRepository subjectStatisticsRepository;
    private final ChildRepository childRepository;
    private final MascotService mascotService;
    
    public BadgeService(
            BadgeRepository badgeRepository,
            ChildBadgeRepository childBadgeRepository,
            SubjectStatisticsRepository subjectStatisticsRepository,
            ChildRepository childRepository,
            MascotService mascotService
    ) {
        this.badgeRepository = badgeRepository;
        this.childBadgeRepository = childBadgeRepository;
        this.subjectStatisticsRepository = subjectStatisticsRepository;
        this.childRepository = childRepository;
        this.mascotService = mascotService;
    }
    
    /**
     * Check and award badges to a child based on their progress
     */
    @Transactional
    public List<BadgeDTO> checkAndAwardBadges(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        List<Badge> allBadges = badgeRepository.findByActiveTrueOrderByDisplayOrder();
        List<BadgeDTO> newlyEarnedBadges = new ArrayList<>();
        
        for (Badge badge : allBadges) {
            // Skip if already earned
            if (childBadgeRepository.existsByChildAndBadge(child, badge)) {
                continue;
            }
            
            // Check if criteria is met
            if (checkBadgeCriteria(child, badge)) {
                // Award the badge
                ChildBadge childBadge = ChildBadge.builder()
                        .child(child)
                        .badge(badge)
                        .build();
                childBadgeRepository.save(childBadge);
                
                log.info("Badge '{}' awarded to child {}", badge.getName(), child.getUsername());
                
                BadgeDTO badgeDTO = toBadgeDTO(badge, childBadge);
                badgeDTO.setIsNew(true);
                newlyEarnedBadges.add(badgeDTO);
            }
        }
        
        return newlyEarnedBadges;
    }
    
    /**
     * Get all badges for a child (earned and unearned)
     */
    public List<BadgeDTO> getChildBadges(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        List<Badge> allBadges = badgeRepository.findByActiveTrueOrderByDisplayOrder();
        List<ChildBadge> earnedBadges = childBadgeRepository.findByChildOrderByEarnedAtDesc(child);
        
        return allBadges.stream().map(badge -> {
            ChildBadge childBadge = earnedBadges.stream()
                    .filter(cb -> cb.getBadge().getId().equals(badge.getId()))
                    .findFirst()
                    .orElse(null);
            
            BadgeDTO dto = toBadgeDTO(badge, childBadge);
            
            // Calculate progress if not earned
            if (childBadge == null) {
                dto.setProgress(calculateProgress(child, badge));
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * Get newly earned badges that haven't been seen
     */
    public List<BadgeDTO> getUnseenBadges(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        List<ChildBadge> unseenBadges = childBadgeRepository
                .findByChildAndNotificationSeenFalseOrderByEarnedAtDesc(child);
        
        return unseenBadges.stream()
                .map(cb -> {
                    BadgeDTO dto = toBadgeDTO(cb.getBadge(), cb);
                    dto.setIsNew(true);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Mark badge notifications as seen
     */
    @Transactional
    public void markBadgesAsSeen(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        List<ChildBadge> unseenBadges = childBadgeRepository
                .findByChildAndNotificationSeenFalseOrderByEarnedAtDesc(child);
        
        unseenBadges.forEach(cb -> cb.setNotificationSeen(true));
        childBadgeRepository.saveAll(unseenBadges);
    }
    
    /**
     * Check if badge criteria is met
     */
    private boolean checkBadgeCriteria(Child child, Badge badge) {
        return switch (badge.getCriteriaType()) {
            case QUESTIONS_ASKED -> checkQuestionsAsked(child, badge);
            case QUIZZES_COMPLETED -> checkQuizzesCompleted(child, badge);
            case PERFECT_SCORES -> checkPerfectScores(child, badge);
            case TOTAL_ACHIEVEMENTS -> checkTotalAchievements(child, badge);
            default -> false;
        };
    }
    
    private boolean checkQuestionsAsked(Child child, Badge badge) {
        if (badge.getSubject() != null) {
            // Subject-specific badge
            return subjectStatisticsRepository.findByChildAndSubject(child, badge.getSubject())
                    .map(stats -> stats.getTotalQuestions() >= badge.getCriteriaValue())
                    .orElse(false);
        }
        // Total questions across all subjects
        return subjectStatisticsRepository.findByChildOrderByTotalQuestionsDesc(child).stream()
                .mapToInt(SubjectStatistics::getTotalQuestions)
                .sum() >= badge.getCriteriaValue();
    }
    
    private boolean checkQuizzesCompleted(Child child, Badge badge) {
        // Use child's total quizzes completed
        return child.getTotalQuizzesCompleted() != null && 
               child.getTotalQuizzesCompleted() >= badge.getCriteriaValue();
    }
    
    private boolean checkPerfectScores(Child child, Badge badge) {
        // This would require tracking perfect scores - for now return false
        // TODO: Add perfect score tracking
        return false;
    }
    
    private boolean checkTotalAchievements(Child child, Badge badge) {
        long totalBadges = childBadgeRepository.countByChild(child);
        return totalBadges >= badge.getCriteriaValue();
    }
    
    /**
     * Calculate progress towards earning a badge (0-100%)
     */
    private Integer calculateProgress(Child child, Badge badge) {
        int current = switch (badge.getCriteriaType()) {
            case QUESTIONS_ASKED -> {
                if (badge.getSubject() != null) {
                    yield subjectStatisticsRepository.findByChildAndSubject(child, badge.getSubject())
                            .map(SubjectStatistics::getTotalQuestions)
                            .orElse(0);
                }
                yield subjectStatisticsRepository.findByChildOrderByTotalQuestionsDesc(child).stream()
                        .mapToInt(SubjectStatistics::getTotalQuestions)
                        .sum();
            }
            case QUIZZES_COMPLETED -> child.getTotalQuizzesCompleted() != null ? child.getTotalQuizzesCompleted() : 0;
            case TOTAL_ACHIEVEMENTS -> (int) childBadgeRepository.countByChild(child);
            default -> 0;
        };
        
        return Math.min(100, (current * 100) / badge.getCriteriaValue());
    }
    
    private BadgeDTO toBadgeDTO(Badge badge, ChildBadge childBadge) {
        return BadgeDTO.builder()
                .id(badge.getId())
                .code(badge.getCode())
                .name(badge.getName())
                .description(badge.getDescription())
                .icon(badge.getIcon())
                .category(badge.getCategory().name())
                .rarity(badge.getRarity().name())
                .subjectName(badge.getSubject() != null ? badge.getSubject().getName() : null)
                .criteriaValue(badge.getCriteriaValue())
                .criteriaType(badge.getCriteriaType().name())
                .earned(childBadge != null)
                .earnedAt(childBadge != null ? childBadge.getEarnedAt() : null)
                .isNew(childBadge != null && !childBadge.getNotificationSeen())
                .build();
    }
}

