package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Achievement badges that students can earn
 */
@Entity
@Table(name = "badges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Emoji or icon for the badge
     */
    @Column(nullable = false)
    private String icon;
    
    @Enumerated(EnumType.STRING)
    private BadgeCategory category;
    
    @Enumerated(EnumType.STRING)
    private BadgeRarity rarity;
    
    /**
     * Subject this badge is related to (null for general badges)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;
    
    /**
     * Criteria type for unlocking this badge
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CriteriaType criteriaType;
    
    /**
     * Threshold value for the criteria
     * e.g., 50 for "50 science questions"
     */
    @Column(nullable = false)
    private Integer criteriaValue;
    
    /**
     * Display order for sorting badges
     */
    private Integer displayOrder;
    
    private Boolean active;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
    
    public enum BadgeCategory {
        SUBJECT_MASTERY,    // Subject-specific achievements
        QUIZ_PERFORMANCE,   // Quiz-related achievements
        STREAK,             // Streak-based achievements
        TIME_BASED,         // Time spent learning
        SPECIAL             // Special achievements
    }
    
    public enum BadgeRarity {
        COMMON,     // Easy to get
        UNCOMMON,   // Moderate difficulty
        RARE,       // Challenging
        EPIC,       // Very challenging
        LEGENDARY   // Extremely rare
    }
    
    public enum CriteriaType {
        QUESTIONS_ASKED,        // Total questions in a subject
        QUIZZES_COMPLETED,      // Total quizzes completed
        PERFECT_SCORES,         // Number of 100% quiz scores
        STREAK_DAYS,            // Consecutive days of activity
        TOTAL_TIME_MINUTES,     // Total time spent learning
        SUBJECT_PROFICIENCY,    // Proficiency level in a subject
        TOTAL_ACHIEVEMENTS      // Total number of other badges
    }
}

