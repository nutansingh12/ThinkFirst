package com.thinkfirst.dto;

import com.thinkfirst.model.Quiz;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    private String message;
    private ResponseType responseType;
    private Quiz quiz;
    private String hint;
    private Long messageId;
    
    public enum ResponseType {
        FULL_ANSWER,           // 70%+ score - full detailed answer
        PARTIAL_HINT,          // 40-70% score - partial answer with hints
        GUIDED_QUESTIONS,      // 0-40% score - only guiding questions
        QUIZ_REQUIRED          // Must complete quiz first
    }
    
    public static ChatResponse withQuiz(Quiz quiz) {
        return ChatResponse.builder()
                .responseType(ResponseType.QUIZ_REQUIRED)
                .quiz(quiz)
                .message("Please complete this quiz before I can help you with that topic!")
                .build();
    }
    
    public static ChatResponse withAnswerAndQuiz(String answer, Quiz verificationQuiz) {
        return ChatResponse.builder()
                .responseType(ResponseType.FULL_ANSWER)
                .message(answer)
                .quiz(verificationQuiz)
                .build();
    }
    
    public static ChatResponse withHint(String hint) {
        return ChatResponse.builder()
                .responseType(ResponseType.PARTIAL_HINT)
                .hint(hint)
                .message("Here's a hint to help you think about this...")
                .build();
    }
}

