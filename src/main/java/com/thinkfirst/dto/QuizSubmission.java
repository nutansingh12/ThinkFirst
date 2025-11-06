package com.thinkfirst.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmission {
    
    @NotNull(message = "Child ID is required")
    private Long childId;
    
    @NotNull(message = "Quiz ID is required")
    private Long quizId;
    
    @NotNull(message = "Answers are required")
    private Map<Long, String> answers; // questionId -> answer
    
    private Integer timeSpentSeconds;
}

