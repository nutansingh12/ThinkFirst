package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks child's statistics per subject for learning profile
 */
@Entity
@Table(name = "subject_statistics", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"child_id", "subject_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    
    /**
     * Total questions asked in this subject
     */
    @Column(nullable = false)
    private Integer totalQuestions;
    
    /**
     * Total quizzes completed in this subject
     */
    @Column(nullable = false)
    private Integer totalQuizzes;
    
    /**
     * Total correct answers in this subject
     */
    @Column(nullable = false)
    private Integer correctAnswers;
    
    /**
     * Total time spent on this subject (in minutes)
     */
    @Column(nullable = false)
    private Integer timeSpentMinutes;
    
    /**
     * Current streak for this subject
     */
    @Column(nullable = false)
    private Integer currentStreak;
    
    /**
     * Best streak achieved for this subject
     */
    @Column(nullable = false)
    private Integer bestStreak;
    
    /**
     * Proficiency level (0-100)
     */
    private Integer proficiencyLevel;
    
    /**
     * Category title based on question count
     * e.g., "Science Enthusiast", "Math Solver"
     */
    private String categoryTitle;
    
    @Column(nullable = false)
    private LocalDateTime lastActivityAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = LocalDateTime.now();
        }
        if (totalQuestions == null) {
            totalQuestions = 0;
        }
        if (totalQuizzes == null) {
            totalQuizzes = 0;
        }
        if (correctAnswers == null) {
            correctAnswers = 0;
        }
        if (timeSpentMinutes == null) {
            timeSpentMinutes = 0;
        }
        if (currentStreak == null) {
            currentStreak = 0;
        }
        if (bestStreak == null) {
            bestStreak = 0;
        }
        if (proficiencyLevel == null) {
            proficiencyLevel = 0;
        }
    }
    
    /**
     * Update category title based on question count
     */
    public void updateCategoryTitle() {
        String subjectName = subject.getName();
        
        if (totalQuestions >= 100) {
            categoryTitle = getCategoryTitle(subjectName, "Master");
        } else if (totalQuestions >= 50) {
            categoryTitle = getCategoryTitle(subjectName, "Expert");
        } else if (totalQuestions >= 25) {
            categoryTitle = getCategoryTitle(subjectName, "Enthusiast");
        } else if (totalQuestions >= 10) {
            categoryTitle = getCategoryTitle(subjectName, "Explorer");
        } else {
            categoryTitle = getCategoryTitle(subjectName, "Beginner");
        }
    }
    
    private String getCategoryTitle(String subject, String level) {
        return switch (subject.toLowerCase()) {
            case "mathematics" -> level.equals("Master") ? "Math Wizard" : 
                                 level.equals("Expert") ? "Math Whiz" :
                                 level.equals("Enthusiast") ? "Math Solver" : 
                                 level.equals("Explorer") ? "Number Explorer" : "Math Beginner";
            case "science" -> level.equals("Master") ? "Young Einstein" :
                             level.equals("Expert") ? "Science Expert" :
                             level.equals("Enthusiast") ? "Science Enthusiast" :
                             level.equals("Explorer") ? "Science Explorer" : "Science Beginner";
            case "history" -> level.equals("Master") ? "History Master" :
                             level.equals("Expert") ? "Time Traveler" :
                             level.equals("Enthusiast") ? "History Buff" :
                             level.equals("Explorer") ? "History Explorer" : "History Beginner";
            case "english" -> level.equals("Master") ? "Word Master" :
                             level.equals("Expert") ? "Grammar Guru" :
                             level.equals("Enthusiast") ? "Language Lover" :
                             level.equals("Explorer") ? "Word Explorer" : "Language Learner";
            case "computer science" -> level.equals("Master") ? "Code Master" :
                                      level.equals("Expert") ? "Tech Wizard" :
                                      level.equals("Enthusiast") ? "Tech Curious" :
                                      level.equals("Explorer") ? "Tech Explorer" : "Tech Beginner";
            default -> subject + " " + level;
        };
    }
}

