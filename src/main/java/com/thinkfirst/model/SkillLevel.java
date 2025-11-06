package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * Tracks a child's proficiency level in a specific subject
 */
@Entity
@Table(name = "skill_levels", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"child_id", "subject_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillLevel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    
    // 0-100 score
    private Integer proficiencyScore = 0;
    
    @Enumerated(EnumType.STRING)
    private DifficultyLevel currentLevel = DifficultyLevel.BEGINNER;
    
    private Integer quizzesCompleted = 0;
    private Integer averageScore = 0;
    
    @LastModifiedDate
    private LocalDateTime lastAssessed;
    
    private Boolean prerequisiteMet = false;
    
    public enum DifficultyLevel {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }
}

