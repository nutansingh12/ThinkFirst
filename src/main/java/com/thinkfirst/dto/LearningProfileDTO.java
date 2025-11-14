package com.thinkfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for child's learning profile showing subject interests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningProfileDTO {
    private List<SubjectProfileDTO> topSubjects;
    private Integer totalQuestions;
    private Integer totalBadges;
    private String currentStreak;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectProfileDTO {
        private String subjectName;
        private String categoryTitle;
        private String icon;
        private Integer questionCount;
        private Integer proficiencyLevel;
        private Integer currentStreak;
    }
}

