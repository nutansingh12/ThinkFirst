package com.thinkfirst.service;

import com.thinkfirst.dto.ProgressReport;
import com.thinkfirst.model.Achievement;
import com.thinkfirst.model.Child;
import com.thinkfirst.model.SkillLevel;
import com.thinkfirst.model.Subject;
import com.thinkfirst.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProgressTrackingService {

    private static final Logger log = LoggerFactory.getLogger(ProgressTrackingService.class);

    private final ChildRepository childRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AchievementRepository achievementRepository;
    private final SubjectRepository subjectRepository;

    public ProgressTrackingService(
            ChildRepository childRepository,
            SkillLevelRepository skillLevelRepository,
            QuizAttemptRepository quizAttemptRepository,
            AchievementRepository achievementRepository,
            SubjectRepository subjectRepository) {
        this.childRepository = childRepository;
        this.skillLevelRepository = skillLevelRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.achievementRepository = achievementRepository;
        this.subjectRepository = subjectRepository;
    }
    
    /**
     * Check if child has met prerequisites for a subject
     */
    public boolean checkPrerequisite(Long childId, Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        
        // If no prerequisites, return true
        if (subject.getPrerequisites().isEmpty()) {
            return true;
        }
        
        // Check if all prerequisites are met
        for (Subject prereq : subject.getPrerequisites()) {
            SkillLevel skillLevel = skillLevelRepository
                    .findByChildIdAndSubjectId(childId, prereq.getId())
                    .orElse(null);
            
            if (skillLevel == null || !skillLevel.getPrerequisiteMet()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Update child's daily streak
     */
    @Transactional
    public void updateStreak(Child child) {
        LocalDateTime lastActive = child.getLastActiveDate();
        LocalDateTime now = LocalDateTime.now();
        
        if (lastActive == null) {
            // First activity
            child.setCurrentStreak(1);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(lastActive.toLocalDate(), now.toLocalDate());
            
            if (daysBetween == 0) {
                // Same day, no change
                return;
            } else if (daysBetween == 1) {
                // Consecutive day, increment streak
                child.setCurrentStreak(child.getCurrentStreak() + 1);
            } else {
                // Streak broken, reset
                child.setCurrentStreak(1);
            }
        }
        
        childRepository.save(child);
    }
    
    /**
     * Get comprehensive progress report for a child
     */
    public ProgressReport getProgressReport(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        List<SkillLevel> skillLevels = skillLevelRepository.findByChildId(childId);
        List<Achievement> recentAchievements = achievementRepository
                .findByChildIdOrderByEarnedAtDesc(childId)
                .stream()
                .limit(5)
                .toList();
        
        Double averageScore = quizAttemptRepository.getAverageScoreByChildId(childId);
        
        Map<String, ProgressReport.SubjectProgress> subjectProgress = new HashMap<>();
        for (SkillLevel skillLevel : skillLevels) {
            subjectProgress.put(
                    skillLevel.getSubject().getName(),
                    ProgressReport.SubjectProgress.builder()
                            .subjectName(skillLevel.getSubject().getName())
                            .proficiencyScore(skillLevel.getProficiencyScore())
                            .currentLevel(skillLevel.getCurrentLevel().name())
                            .quizzesCompleted(skillLevel.getQuizzesCompleted())
                            .averageScore(skillLevel.getAverageScore())
                            .build()
            );
        }
        
        return ProgressReport.builder()
                .childId(child.getId())
                .childUsername(child.getUsername())
                .currentStreak(child.getCurrentStreak())
                .totalQuizzesCompleted(child.getTotalQuizzesCompleted())
                .totalQuestionsAnswered(child.getTotalQuestionsAnswered())
                .averageScore(averageScore != null ? averageScore : 0.0)
                .skillLevels(skillLevels)
                .recentAchievements(recentAchievements)
                .subjectProgress(subjectProgress)
                .build();
    }
}

