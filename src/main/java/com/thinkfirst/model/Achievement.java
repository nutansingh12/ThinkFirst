package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * Achievement/Badge earned by a child
 */
@Entity
@Table(name = "achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;
    
    private String badgeName; // "Math Master", "Science Explorer"
    private String description;
    private String iconUrl;
    
    @Enumerated(EnumType.STRING)
    private AchievementType type;
    
    private Integer points = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    protected void onCreate() {
        if (earnedAt == null) {
            earnedAt = LocalDateTime.now();
        }
    }

    public enum AchievementType {
        SUBJECT_MASTERY,      // Achieved 90%+ in a subject
        STREAK_MILESTONE,     // 7, 14, 30 day streaks
        QUIZ_PERFECT,         // 100% on a quiz
        QUICK_LEARNER,        // Completed quiz in under 2 minutes
        PERSISTENT_LEARNER,   // Completed 50+ quizzes
        FIRST_STEPS           // Completed first quiz
    }
}

