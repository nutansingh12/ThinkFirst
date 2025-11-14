package com.thinkfirst.dto;

import com.thinkfirst.model.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for quiz generation result containing questions and detected subject
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizGenerationResult {
    
    /**
     * The detected subject from the query
     * e.g., "Mathematics", "Science", "History", etc.
     */
    private String detectedSubject;
    
    /**
     * The generated quiz questions
     */
    private List<Question> questions;
}

