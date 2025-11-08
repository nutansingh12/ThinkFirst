package com.thinkfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildResponse {
    
    private Long id;
    private String username;
    private Integer age;
    private String gradeLevel;
    private Long parentId;
    private Integer currentStreak;
    private Integer totalQuestionsAnswered;
    private Integer totalQuizzesCompleted;
    private LocalDateTime lastActiveDate;
    private Boolean active;
    private LocalDateTime createdAt;
}

