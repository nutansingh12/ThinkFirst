package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz entity - collection of questions
 */
@Entity
@Table(name = "quizzes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty;
    
    @Enumerated(EnumType.STRING)
    private QuizType type; // PREREQUISITE, VERIFICATION, CHALLENGE
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();
    
    private Integer passingScore = 70;
    private Integer timeLimit; // in seconds, null = no limit
    
    private String title;
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private Boolean active = true;
    
    public enum DifficultyLevel {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }
    
    public enum QuizType {
        PREREQUISITE,    // Must pass before getting full answer
        VERIFICATION,    // After receiving answer to verify understanding
        CHALLENGE,       // Optional challenge quiz
        DIAGNOSTIC       // Initial assessment
    }
}

