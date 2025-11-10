package com.thinkfirst.service;

import com.thinkfirst.dto.ChatResponse;
import com.thinkfirst.dto.QuizResult;
import com.thinkfirst.dto.QuizSubmission;
import com.thinkfirst.model.*;
import com.thinkfirst.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    public final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ChildRepository childRepository;
    private final SubjectRepository subjectRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final com.thinkfirst.service.ai.AIProviderService aiProviderService;
    private final AchievementService achievementService;

    @Value("${app.quiz.passing-score}")
    private Integer passingScore;

    @Value("${app.quiz.default-question-count}")
    private Integer defaultQuestionCount;

    public QuizService(
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            ChildRepository childRepository,
            SubjectRepository subjectRepository,
            SkillLevelRepository skillLevelRepository,
            com.thinkfirst.service.ai.AIProviderService aiProviderService,
            AchievementService achievementService) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.childRepository = childRepository;
        this.subjectRepository = subjectRepository;
        this.skillLevelRepository = skillLevelRepository;
        this.aiProviderService = aiProviderService;
        this.achievementService = achievementService;
    }
    
    /**
     * Generate a prerequisite quiz for a subject
     */
    @Transactional
    public Quiz generatePrerequisiteQuiz(Long childId, Subject subject, String query) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        SkillLevel skillLevel = getOrCreateSkillLevel(child, subject);

        // Generate questions using AI provider (with automatic fallback)
        List<Question> questions = aiProviderService.generateQuestions(
                query,
                subject.getName(),
                defaultQuestionCount,
                skillLevel.getCurrentLevel().name()
        );
        
        Quiz quiz = Quiz.builder()
                .subject(subject)
                .difficulty(Quiz.DifficultyLevel.valueOf(skillLevel.getCurrentLevel().name()))
                .type(Quiz.QuizType.PREREQUISITE)
                .passingScore(passingScore)
                .title("Prerequisites for " + subject.getName())
                .description("Complete this quiz to unlock full answers about " + subject.getName())
                .active(true)
                .build();
        
        quiz = quizRepository.save(quiz);
        
        // Associate questions with quiz
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            q.setQuiz(quiz);
            q.setDisplayOrder(i);
        }
        quiz.setQuestions(questions);
        
        return quizRepository.save(quiz);
    }
    
    /**
     * Generate verification quiz after providing an answer
     */
    @Transactional
    public Quiz generateVerificationQuiz(String query, String answer, Long childId, Subject subject) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        // Generate 2-3 quick verification questions using AI provider
        List<Question> questions = aiProviderService.generateQuestions(
                "Verification questions for: " + query,
                subject.getName(),
                3,
                "BEGINNER"
        );
        
        Quiz quiz = Quiz.builder()
                .subject(subject)
                .difficulty(Quiz.DifficultyLevel.BEGINNER)
                .type(Quiz.QuizType.VERIFICATION)
                .passingScore(70)
                .title("Quick Check")
                .description("Let's verify you understood the concept!")
                .active(true)
                .build();
        
        quiz = quizRepository.save(quiz);
        
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            q.setQuiz(quiz);
            q.setDisplayOrder(i);
        }
        quiz.setQuestions(questions);
        
        return quizRepository.save(quiz);
    }
    
    /**
     * Evaluate a quiz submission
     */
    @Transactional
    public QuizResult evaluateQuiz(QuizSubmission submission) {
        Child child = childRepository.findById(submission.getChildId())
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        Quiz quiz = quizRepository.findById(submission.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        Map<Long, String> answers = submission.getAnswers();
        List<QuizResult.QuestionResult> questionResults = new ArrayList<>();
        int correctAnswers = 0;
        
        // Evaluate each question
        for (Question question : quiz.getQuestions()) {
            String userAnswer = answers.get(question.getId());
            boolean isCorrect = checkAnswer(question, userAnswer);
            
            if (isCorrect) {
                correctAnswers++;
            }
            
            questionResults.add(QuizResult.QuestionResult.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .userAnswer(userAnswer)
                    .correctAnswer(question.getCorrectAnswer())
                    .correct(isCorrect)
                    .explanation(question.getExplanation())
                    .build());
        }
        
        // Calculate score
        int totalQuestions = quiz.getQuestions().size();
        int score = (correctAnswers * 100) / totalQuestions;
        boolean passed = score >= quiz.getPassingScore();
        
        // Save attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .child(child)
                .quiz(quiz)
                .score(score)
                .passed(passed)
                .answers(answers)
                .timeSpentSeconds(submission.getTimeSpentSeconds())
                .completedAt(LocalDateTime.now())
                .feedbackMessage(generateFeedback(score, passed))
                .build();
        
        attempt = quizAttemptRepository.save(attempt);
        
        // Update skill level
        updateSkillLevel(child, quiz.getSubject(), score, passed);
        
        // Update child stats
        child.setTotalQuizzesCompleted(child.getTotalQuizzesCompleted() + 1);
        child.setTotalQuestionsAnswered(child.getTotalQuestionsAnswered() + totalQuestions);
        child.setLastActiveDate(LocalDateTime.now());
        childRepository.save(child);
        
        // Check for achievements
        achievementService.checkAndAwardAchievements(child, score, passed);
        
        // Determine response level
        ChatResponse.ResponseType responseLevel = determineResponseLevel(score);
        
        return QuizResult.builder()
                .attemptId(attempt.getId())
                .score(score)
                .passed(passed)
                .responseLevel(responseLevel)
                .feedbackMessage(attempt.getFeedbackMessage())
                .questionResults(questionResults)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .build();
    }
    
    private boolean checkAnswer(Question question, String userAnswer) {
        if (userAnswer == null) {
            return false;
        }
        
        switch (question.getType()) {
            case MULTIPLE_CHOICE:
                // Check if the answer matches the correct option
                if (question.getCorrectOptionIndex() != null) {
                    try {
                        int userIndex = Integer.parseInt(userAnswer);
                        return userIndex == question.getCorrectOptionIndex();
                    } catch (NumberFormatException e) {
                        // Try matching text
                        return userAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
                    }
                }
                return userAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
                
            case TRUE_FALSE:
                return userAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
                
            case SHORT_ANSWER:
            case FILL_BLANK:
                // Simple string matching (could be enhanced with fuzzy matching)
                return userAnswer.trim().toLowerCase().contains(
                        question.getCorrectAnswer().trim().toLowerCase()
                );
                
            default:
                return false;
        }
    }
    
    private ChatResponse.ResponseType determineResponseLevel(int score) {
        if (score >= 70) {
            return ChatResponse.ResponseType.FULL_ANSWER;
        } else if (score >= 40) {
            return ChatResponse.ResponseType.PARTIAL_HINT;
        } else {
            return ChatResponse.ResponseType.GUIDED_QUESTIONS;
        }
    }
    
    private String generateFeedback(int score, boolean passed) {
        if (score >= 90) {
            return "Excellent work! You really understand this topic! 🌟";
        } else if (score >= 70) {
            return "Great job! You've passed and can now get full answers! 👍";
        } else if (score >= 50) {
            return "Good effort! You're getting there. I'll give you some hints to help you learn more.";
        } else {
            return "Keep trying! Let me guide you with some questions to help you think about this.";
        }
    }
    
    private void updateSkillLevel(Child child, Subject subject, int score, boolean passed) {
        SkillLevel skillLevel = getOrCreateSkillLevel(child, subject);
        
        // Update stats
        skillLevel.setQuizzesCompleted(skillLevel.getQuizzesCompleted() + 1);
        
        // Calculate new average score
        int currentAvg = skillLevel.getAverageScore();
        int quizCount = skillLevel.getQuizzesCompleted();
        int newAvg = ((currentAvg * (quizCount - 1)) + score) / quizCount;
        skillLevel.setAverageScore(newAvg);
        
        // Update proficiency score (weighted towards recent performance)
        int newProficiency = (int) (skillLevel.getProficiencyScore() * 0.7 + score * 0.3);
        skillLevel.setProficiencyScore(newProficiency);
        
        // Update difficulty level based on proficiency
        if (newProficiency >= 80) {
            skillLevel.setCurrentLevel(SkillLevel.DifficultyLevel.EXPERT);
        } else if (newProficiency >= 60) {
            skillLevel.setCurrentLevel(SkillLevel.DifficultyLevel.ADVANCED);
        } else if (newProficiency >= 40) {
            skillLevel.setCurrentLevel(SkillLevel.DifficultyLevel.INTERMEDIATE);
        }
        
        skillLevel.setPrerequisiteMet(passed);
        skillLevel.setLastAssessed(LocalDateTime.now());
        
        skillLevelRepository.save(skillLevel);
    }
    
    private SkillLevel getOrCreateSkillLevel(Child child, Subject subject) {
        return skillLevelRepository.findByChildIdAndSubjectId(child.getId(), subject.getId())
                .orElseGet(() -> {
                    SkillLevel newSkillLevel = SkillLevel.builder()
                            .child(child)
                            .subject(subject)
                            .proficiencyScore(0)
                            .currentLevel(SkillLevel.DifficultyLevel.BEGINNER)
                            .quizzesCompleted(0)
                            .averageScore(0)
                            .prerequisiteMet(false)
                            .build();
                    return skillLevelRepository.save(newSkillLevel);
                });
    }
}

