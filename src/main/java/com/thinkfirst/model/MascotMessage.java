package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Quizzy the Owl - ThinkFirst's friendly mascot
 * Provides encouraging messages and guidance to students
 */
@Entity
@Table(name = "mascot_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MascotMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;
    
    @Enumerated(EnumType.STRING)
    private MessageContext context;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    /**
     * Optional subject this message is related to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;
    
    /**
     * Minimum score to trigger this message (for quiz results)
     */
    private Integer minScore;
    
    /**
     * Maximum score to trigger this message (for quiz results)
     */
    private Integer maxScore;
    
    /**
     * Priority for message selection (higher = more likely to show)
     */
    private Integer priority;
    
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
        if (priority == null) {
            priority = 1;
        }
    }
    
    public enum MessageType {
        WELCOME,           // First time greeting
        ENCOURAGEMENT,     // General encouragement
        QUIZ_START,        // Before taking a quiz
        QUIZ_SUCCESS,      // After passing a quiz
        QUIZ_PARTIAL,      // After partial success (40-69%)
        QUIZ_FAIL,         // After failing a quiz
        STREAK_MILESTONE,  // Streak achievements
        SUBJECT_HELP,      // Subject-specific guidance
        ACHIEVEMENT_UNLOCK,// When unlocking a badge
        DAILY_GREETING,    // Daily login message
        HINT_GIVEN         // When providing a hint
    }
    
    public enum MessageContext {
        MATHEMATICS,
        SCIENCE,
        HISTORY,
        ENGLISH,
        GEOGRAPHY,
        COMPUTER_SCIENCE,
        ART,
        MUSIC,
        GENERAL
    }
}

