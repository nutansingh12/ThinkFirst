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
    private final ChatMessageRepository chatMessageRepository;
    private final com.thinkfirst.service.ai.AIProviderService aiProviderService;
    private final AchievementService achievementService;
    private final LearningPathService learningPathService;
    private final ProgressTrackingService progressTrackingService;

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
            ChatMessageRepository chatMessageRepository,
            com.thinkfirst.service.ai.AIProviderService aiProviderService,
            AchievementService achievementService,
            LearningPathService learningPathService,
            ProgressTrackingService progressTrackingService) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.childRepository = childRepository;
        this.subjectRepository = subjectRepository;
        this.skillLevelRepository = skillLevelRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiProviderService = aiProviderService;
        this.achievementService = achievementService;
        this.learningPathService = learningPathService;
        this.progressTrackingService = progressTrackingService;
    }
    
    /**
     * Get a quiz by ID
     */
    public Quiz getQuizById(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found with ID: " + quizId));
    }

    /**
     * Generate a prerequisite quiz for a subject
     */
    @Transactional
    public Quiz generatePrerequisiteQuiz(Long childId, Integer age, Subject subject, String query) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        SkillLevel skillLevel = getOrCreateSkillLevel(child, subject);

        // Generate questions using AI provider (with automatic fallback)
        List<Question> questions = aiProviderService.generateQuestions(
                query,
                subject.getName(),
                defaultQuestionCount,
                skillLevel.getCurrentLevel().name(),
                age
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
        long startTime = System.currentTimeMillis();
        log.info("Starting verification quiz generation for query: {}", query);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));

        // Generate 2-3 quick verification questions using AI provider
        long aiStartTime = System.currentTimeMillis();
        List<Question> questions = aiProviderService.generateQuestions(
                "Verification questions for: " + query,
                subject.getName(),
                3,
                SkillLevel.DifficultyLevel.BEGINNER.name(),
                child.getAge()
        );
        long aiEndTime = System.currentTimeMillis();
        log.info("AI question generation took {} ms", (aiEndTime - aiStartTime));
        
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

        Quiz savedQuiz = quizRepository.save(quiz);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Verification quiz generation completed in {} ms (total)", totalTime);

        return savedQuiz;
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

            // Don't add to results yet - we'll add them after we know if student passed
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

        // Filter question results based on score
        // Only show correct answers and explanations if student passed
        List<QuizResult.QuestionResult> filteredResults = new ArrayList<>();
        if (passed) {
            // Student passed - show everything
            filteredResults = questionResults;
        } else {
            // Student failed - hide correct answers and explanations
            for (QuizResult.QuestionResult result : questionResults) {
                filteredResults.add(QuizResult.QuestionResult.builder()
                        .questionId(result.getQuestionId())
                        .questionText(result.getQuestionText())
                        .userAnswer(result.getUserAnswer())
                        .correctAnswer(null)  // Hide correct answer
                        .correct(result.getCorrect())
                        .explanation(null)  // Hide explanation
                        .build());
            }
        }
        
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

        // Check if this is a retake quiz (has originalQuiz reference)
        boolean isRetakeQuiz = quiz.getOriginalQuiz() != null;

        // Update skill level (only for original quizzes, not retakes)
        if (!isRetakeQuiz) {
            updateSkillLevel(child, quiz.getSubject(), score, passed);
        }

        // Update child stats
        // Handle null values for existing children that might not have these fields initialized
        Integer currentQuizzes = child.getTotalQuizzesCompleted() != null ? child.getTotalQuizzesCompleted() : 0;
        Integer currentQuestions = child.getTotalQuestionsAnswered() != null ? child.getTotalQuestionsAnswered() : 0;
        Integer currentTimeMinutes = child.getTotalTimeSpentMinutes() != null ? child.getTotalTimeSpentMinutes() : 0;

        // Only increment quiz count for original quizzes, not retakes
        if (!isRetakeQuiz) {
            child.setTotalQuizzesCompleted(currentQuizzes + 1);
            log.info("Updating child {} stats: quizzes {} -> {}, questions {} -> {}",
                    child.getId(), currentQuizzes, currentQuizzes + 1,
                    currentQuestions, currentQuestions + totalQuestions);
        } else {
            log.info("Retake quiz - NOT incrementing quiz count for child {}", child.getId());
        }

        child.setTotalQuestionsAnswered(currentQuestions + totalQuestions);
        child.setLastActiveDate(LocalDateTime.now());

        // Update streak
        progressTrackingService.updateStreak(child);

        // Update total time spent (convert seconds to minutes for storage)
        log.info("Time tracking - submission.timeSpentSeconds: {}", submission.getTimeSpentSeconds());
        Integer timeSpentMinutes = submission.getTimeSpentSeconds() != null ?
                (submission.getTimeSpentSeconds() / 60) : 0;
        log.info("Time tracking - calculated timeSpentMinutes: {}, currentTimeMinutes: {}",
                timeSpentMinutes, currentTimeMinutes);
        child.setTotalTimeSpentMinutes(currentTimeMinutes + timeSpentMinutes);

        Child savedChild = childRepository.save(child);

        log.info("Child {} stats after save: quizzes={}, questions={}, timeMinutes={}, isRetake={}",
                savedChild.getId(), savedChild.getTotalQuizzesCompleted(),
                savedChild.getTotalQuestionsAnswered(), savedChild.getTotalTimeSpentMinutes(), isRetakeQuiz);

        // Check for achievements (only for original quizzes, not retakes)
        if (!isRetakeQuiz) {
            achievementService.checkAndAwardAchievements(child, score, passed);
        }

        // Get the answer message if student passed (for verification quizzes)
        String answerMessage = null;
        if (passed && quiz.getType() == Quiz.QuizType.VERIFICATION) {
            // For retake quizzes, get the answer from the original quiz
            Quiz quizToGetAnswerFrom = quiz.getOriginalQuiz() != null ? quiz.getOriginalQuiz() : quiz;

            // Find the chat message that contains the answer
            answerMessage = chatMessageRepository.findByAssociatedQuizId(quizToGetAnswerFrom.getId())
                    .map(ChatMessage::getContent)
                    .orElse(null);

            log.info("Student passed quiz {} (original: {}), retrieving answer message (length: {} chars)",
                    quiz.getId(), quizToGetAnswerFrom.getId(),
                    answerMessage != null ? answerMessage.length() : 0);
        }

        // Generate hint and retake quiz if student scored 40-69% (for verification quizzes)
        String hintMessage = null;
        Long retakeQuizId = null;
        if (!passed && score >= 40 && score < 70 && quiz.getType() == Quiz.QuizType.VERIFICATION) {
            try {
                // Collect the questions that were answered incorrectly
                List<String> incorrectQuestions = new ArrayList<>();
                List<Question> incorrectQuestionObjects = new ArrayList<>();
                for (QuizResult.QuestionResult result : questionResults) {
                    if (!result.getCorrect()) {
                        incorrectQuestions.add(result.getQuestionText());
                        // Find the original question object
                        for (Question q : quiz.getQuestions()) {
                            if (q.getId().equals(result.getQuestionId())) {
                                incorrectQuestionObjects.add(q);
                                break;
                            }
                        }
                    }
                }

                log.info("Generating hint for quiz {} (score: {}%) - {} incorrect questions",
                        quiz.getId(), score, incorrectQuestions.size());

                // Build a prompt that focuses on the incorrect questions
                StringBuilder hintPrompt = new StringBuilder();
                hintPrompt.append("The student answered these questions incorrectly:\n");
                for (int i = 0; i < incorrectQuestions.size(); i++) {
                    hintPrompt.append((i + 1)).append(". ").append(incorrectQuestions.get(i)).append("\n");
                }
                hintPrompt.append("\nProvide a helpful hint that guides them toward understanding these concepts without giving away the answers directly.");

                // Generate hint using AI focused on incorrect questions
                hintMessage = aiProviderService.generateHint(
                        hintPrompt.toString(),
                        quiz.getSubject().getName(),
                        child.getAge()
                );

                log.info("Generated hint for child {} focusing on {} incorrect questions (length: {} chars)",
                        child.getId(), incorrectQuestions.size(), hintMessage != null ? hintMessage.length() : 0);

                // Create a retake quiz with only the incorrect questions
                if (!incorrectQuestionObjects.isEmpty()) {
                    // Find the original quiz (in case this is already a retake)
                    Quiz originalQuiz = quiz.getOriginalQuiz() != null ? quiz.getOriginalQuiz() : quiz;

                    Quiz retakeQuiz = Quiz.builder()
                            .subject(quiz.getSubject())
                            .difficulty(quiz.getDifficulty())
                            .passingScore(quiz.getPassingScore())
                            .type(Quiz.QuizType.VERIFICATION)
                            .originalQuiz(originalQuiz)  // Link back to original quiz
                            .build();

                    retakeQuiz = quizRepository.save(retakeQuiz);

                    // Clone the incorrect questions for the retake quiz
                    List<Question> retakeQuestions = new ArrayList<>();
                    for (int i = 0; i < incorrectQuestionObjects.size(); i++) {
                        Question original = incorrectQuestionObjects.get(i);
                        Question cloned = Question.builder()
                                .quiz(retakeQuiz)
                                .questionText(original.getQuestionText())
                                .type(original.getType())
                                .correctAnswer(original.getCorrectAnswer())
                                .correctOptionIndex(original.getCorrectOptionIndex())
                                .options(new ArrayList<>(original.getOptions()))
                                .explanation(original.getExplanation())
                                .displayOrder(i)
                                .build();
                        retakeQuestions.add(cloned);
                    }

                    retakeQuiz.setQuestions(retakeQuestions);
                    retakeQuiz = quizRepository.save(retakeQuiz);
                    retakeQuizId = retakeQuiz.getId();

                    log.info("Created retake quiz {} with {} incorrect questions for child {}",
                            retakeQuizId, incorrectQuestionObjects.size(), child.getId());
                }
            } catch (Exception e) {
                log.error("Failed to generate hint or retake quiz: {}", e.getMessage(), e);
                hintMessage = "You're making progress! Review the questions you got wrong and try to understand the concepts better.";
            }
        }

        // Generate learning path if student failed badly (score < 40%)
        com.thinkfirst.dto.LearningPathResponse learningPath = null;
        if (!passed && score < 40 && quiz.getType() == Quiz.QuizType.VERIFICATION) {
            try {
                // Get the original USER query (not the AI-generated answer)
                // Take the first (most recent) USER message before the quiz
                List<ChatMessage> userMessages = chatMessageRepository.findUserMessagesBeforeQuiz(quiz.getId());
                String originalQuery = userMessages.isEmpty() ? "this topic" : userMessages.get(0).getContent();

                log.info("Found original user query for quiz {}: {}", quiz.getId(),
                        originalQuery.length() > 50 ? originalQuery.substring(0, 50) + "..." : originalQuery);

                learningPath = learningPathService.generateLearningPath(
                        child.getId(),
                        quiz,
                        originalQuery,
                        score,
                        totalQuestions,
                        correctAnswers
                );
                log.info("Generated learning path for child {} with {} lessons",
                        child.getId(), learningPath.getTotalLessons());
            } catch (Exception e) {
                log.error("Failed to generate learning path: {}", e.getMessage(), e);
            }
        }

        // Determine response level
        ChatResponse.ResponseType responseLevel = determineResponseLevel(score);

        return QuizResult.builder()
                .attemptId(attempt.getId())
                .score(score)
                .passed(passed)
                .responseLevel(responseLevel)
                .feedbackMessage(attempt.getFeedbackMessage())
                .answerMessage(answerMessage)
                .hintMessage(hintMessage)  // Include hint if generated
                .questionResults(filteredResults)  // Use filtered results
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .learningPath(learningPath)  // Include learning path if generated
                .retakeQuizId(retakeQuizId)  // Include retake quiz ID if generated
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

