package com.thinkfirst.service;

import com.thinkfirst.dto.LearningProfileDTO;
import com.thinkfirst.model.Child;
import com.thinkfirst.model.Subject;
import com.thinkfirst.model.SubjectStatistics;
import com.thinkfirst.repository.ChildBadgeRepository;
import com.thinkfirst.repository.ChildRepository;
import com.thinkfirst.repository.SubjectRepository;
import com.thinkfirst.repository.SubjectStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for tracking subject-specific statistics
 */
@Service
public class SubjectStatisticsService {
    
    private static final Logger log = LoggerFactory.getLogger(SubjectStatisticsService.class);
    
    private final SubjectStatisticsRepository subjectStatisticsRepository;
    private final ChildRepository childRepository;
    private final SubjectRepository subjectRepository;
    private final ChildBadgeRepository childBadgeRepository;
    
    public SubjectStatisticsService(
            SubjectStatisticsRepository subjectStatisticsRepository,
            ChildRepository childRepository,
            SubjectRepository subjectRepository,
            ChildBadgeRepository childBadgeRepository
    ) {
        this.subjectStatisticsRepository = subjectStatisticsRepository;
        this.childRepository = childRepository;
        this.subjectRepository = subjectRepository;
        this.childBadgeRepository = childBadgeRepository;
    }
    
    /**
     * Record a question asked in a subject
     */
    @Transactional
    public void recordQuestion(Long childId, Long subjectId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        
        SubjectStatistics stats = subjectStatisticsRepository
                .findByChildAndSubject(child, subject)
                .orElse(SubjectStatistics.builder()
                        .child(child)
                        .subject(subject)
                        .build());
        
        stats.setTotalQuestions(stats.getTotalQuestions() + 1);
        stats.setLastActivityAt(LocalDateTime.now());
        stats.updateCategoryTitle();
        
        subjectStatisticsRepository.save(stats);
        log.info("Recorded question for child {} in subject {}", child.getUsername(), subject.getName());
    }
    
    /**
     * Record a quiz completion in a subject
     */
    @Transactional
    public void recordQuiz(Long childId, Long subjectId, int score, int timeSpentMinutes) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        
        SubjectStatistics stats = subjectStatisticsRepository
                .findByChildAndSubject(child, subject)
                .orElse(SubjectStatistics.builder()
                        .child(child)
                        .subject(subject)
                        .build());
        
        stats.setTotalQuizzes(stats.getTotalQuizzes() + 1);
        stats.setTimeSpentMinutes(stats.getTimeSpentMinutes() + timeSpentMinutes);
        stats.setLastActivityAt(LocalDateTime.now());
        
        // Update streak
        if (score >= 70) {
            stats.setCurrentStreak(stats.getCurrentStreak() + 1);
            if (stats.getCurrentStreak() > stats.getBestStreak()) {
                stats.setBestStreak(stats.getCurrentStreak());
            }
        } else {
            stats.setCurrentStreak(0);
        }
        
        stats.updateCategoryTitle();
        subjectStatisticsRepository.save(stats);
        
        log.info("Recorded quiz for child {} in subject {} with score {}", 
                child.getUsername(), subject.getName(), score);
    }
    
    /**
     * Get learning profile for a child
     */
    public LearningProfileDTO getLearningProfile(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        List<SubjectStatistics> topSubjects = subjectStatisticsRepository
                .findTop5ByChildOrderByTotalQuestionsDesc(child);
        
        int totalQuestions = subjectStatisticsRepository
                .findByChildOrderByTotalQuestionsDesc(child).stream()
                .mapToInt(SubjectStatistics::getTotalQuestions)
                .sum();
        
        long totalBadges = childBadgeRepository.countByChild(child);
        
        // Calculate current overall streak
        int currentStreak = topSubjects.stream()
                .mapToInt(SubjectStatistics::getCurrentStreak)
                .max()
                .orElse(0);
        
        List<LearningProfileDTO.SubjectProfileDTO> subjectProfiles = topSubjects.stream()
                .map(stats -> LearningProfileDTO.SubjectProfileDTO.builder()
                        .subjectName(stats.getSubject().getName())
                        .categoryTitle(stats.getCategoryTitle())
                        .icon(getSubjectIcon(stats.getSubject().getName()))
                        .questionCount(stats.getTotalQuestions())
                        .proficiencyLevel(stats.getProficiencyLevel())
                        .currentStreak(stats.getCurrentStreak())
                        .build())
                .collect(Collectors.toList());
        
        return LearningProfileDTO.builder()
                .topSubjects(subjectProfiles)
                .totalQuestions(totalQuestions)
                .totalBadges((int) totalBadges)
                .currentStreak(currentStreak + " day" + (currentStreak != 1 ? "s" : ""))
                .build();
    }
    
    private String getSubjectIcon(String subjectName) {
        return switch (subjectName.toLowerCase()) {
            case "mathematics" -> "➗";
            case "science" -> "🔬";
            case "history" -> "📜";
            case "english" -> "📖";
            case "geography" -> "🌍";
            case "computer science" -> "💻";
            case "art" -> "🎨";
            case "music" -> "🎵";
            default -> "📚";
        };
    }
}

