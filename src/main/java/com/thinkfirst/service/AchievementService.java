package com.thinkfirst.service;

import com.thinkfirst.model.Achievement;
import com.thinkfirst.model.Child;
import com.thinkfirst.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AchievementService {
    
    private final AchievementRepository achievementRepository;
    
    /**
     * Check and award achievements based on quiz performance
     */
    @Transactional
    public void checkAndAwardAchievements(Child child, Integer score, Boolean passed) {
        // First quiz completion
        if (child.getTotalQuizzesCompleted() == 1) {
            awardAchievement(child, "First Steps", "Completed your first quiz!", 
                    Achievement.AchievementType.FIRST_STEPS, 10);
        }
        
        // Perfect score
        if (score == 100) {
            awardAchievement(child, "Perfect Score", "Got 100% on a quiz!", 
                    Achievement.AchievementType.QUIZ_PERFECT, 50);
        }
        
        // Streak milestones
        if (child.getCurrentStreak() == 7) {
            awardAchievement(child, "Week Warrior", "7 day learning streak!", 
                    Achievement.AchievementType.STREAK_MILESTONE, 100);
        } else if (child.getCurrentStreak() == 30) {
            awardAchievement(child, "Month Master", "30 day learning streak!", 
                    Achievement.AchievementType.STREAK_MILESTONE, 500);
        }
        
        // Quiz completion milestones
        if (child.getTotalQuizzesCompleted() == 10) {
            awardAchievement(child, "Quiz Novice", "Completed 10 quizzes!", 
                    Achievement.AchievementType.PERSISTENT_LEARNER, 50);
        } else if (child.getTotalQuizzesCompleted() == 50) {
            awardAchievement(child, "Quiz Expert", "Completed 50 quizzes!", 
                    Achievement.AchievementType.PERSISTENT_LEARNER, 200);
        } else if (child.getTotalQuizzesCompleted() == 100) {
            awardAchievement(child, "Quiz Master", "Completed 100 quizzes!", 
                    Achievement.AchievementType.PERSISTENT_LEARNER, 500);
        }
    }
    
    private void awardAchievement(Child child, String badgeName, String description, 
                                   Achievement.AchievementType type, Integer points) {
        // Check if already awarded
        List<Achievement> existing = achievementRepository.findByChildIdAndType(child.getId(), type);
        boolean alreadyHas = existing.stream()
                .anyMatch(a -> a.getBadgeName().equals(badgeName));
        
        if (!alreadyHas) {
            Achievement achievement = Achievement.builder()
                    .child(child)
                    .badgeName(badgeName)
                    .description(description)
                    .type(type)
                    .points(points)
                    .build();
            
            achievementRepository.save(achievement);
            log.info("Awarded achievement '{}' to child {}", badgeName, child.getUsername());
        }
    }
    
    public List<Achievement> getChildAchievements(Long childId) {
        return achievementRepository.findByChildIdOrderByEarnedAtDesc(childId);
    }
}

