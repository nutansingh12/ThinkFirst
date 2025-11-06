package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Question entity - individual quiz question
 */
@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnore
    private Quiz quiz;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;
    
    @Enumerated(EnumType.STRING)
    private QuestionType type; // MULTIPLE_CHOICE, SHORT_ANSWER, TRUE_FALSE
    
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String correctAnswer;
    
    private Integer correctOptionIndex; // For multiple choice
    
    private String explanation; // Why this is correct
    
    private Integer displayOrder;
    
    public enum QuestionType {
        MULTIPLE_CHOICE, SHORT_ANSWER, TRUE_FALSE, FILL_BLANK
    }
}

