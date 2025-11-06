package com.thinkfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResult {
    
    private Long attemptId;
    private Integer score;
    private Boolean passed;
    private ChatResponse.ResponseType responseLevel;
    private String feedbackMessage;
    private List<QuestionResult> questionResults;
    private Integer totalQuestions;
    private Integer correctAnswers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private Long questionId;
        private String questionText;
        private String userAnswer;
        private String correctAnswer;
        private Boolean correct;
        private String explanation;
    }
}

