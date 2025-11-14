package com.thinkfirst.dto;

import com.thinkfirst.model.Achievement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReport {

    private Long childId;
    private String childUsername;
    private Integer currentStreak;
    private Integer totalQuizzesCompleted;
    private Integer totalQuestionsAnswered;
    private Integer totalTimeSpentMinutes;
    private Double averageScore;
    private List<Achievement> recentAchievements;
    private Map<String, SubjectProgress> subjectProgress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectProgress {
        private String subjectName;
        private Integer proficiencyScore;
        private String currentLevel;
        private Integer quizzesCompleted;
        private Integer averageScore;
    }
}

