package com.thinkfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for learning path response to frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponse {

    private Long id;
    private String topic;
    private String originalQuery;
    private Integer score;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private String motivationalMessage;
    private List<LessonDTO> lessons;
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer progressPercentage;
    private String proTip;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonDTO {
        private Long id;
        private String title;
        private String description;
        private String content;
        private Integer displayOrder;
        private List<ResourceDTO> resources;
        private Boolean completed;
        private Boolean locked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceDTO {
        private String type;
        private String title;
        private String url;
        private String description;
    }
}

