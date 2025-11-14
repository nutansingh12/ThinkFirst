package com.thinkfirst.service;

import com.thinkfirst.dto.ChatRequest;
import com.thinkfirst.dto.ChatResponse;
import com.thinkfirst.dto.ModerationResult;
import com.thinkfirst.model.*;
import com.thinkfirst.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core service implementing quiz-gated chat logic
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChildRepository childRepository;
    private final SubjectRepository subjectRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final com.thinkfirst.service.ai.AIProviderService aiProviderService;
    private final QuizService quizService;
    private final ProgressTrackingService progressTrackingService;
    private final ContentModerationService contentModerationService;
    private final LearningPathService learningPathService;
    private final MascotService mascotService;
    private final SubjectStatisticsService subjectStatisticsService;
    private final BadgeService badgeService;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            ChildRepository childRepository,
            SubjectRepository subjectRepository,
            SkillLevelRepository skillLevelRepository,
            com.thinkfirst.service.ai.AIProviderService aiProviderService,
            QuizService quizService,
            ProgressTrackingService progressTrackingService,
            ContentModerationService contentModerationService,
            LearningPathService learningPathService,
            MascotService mascotService,
            SubjectStatisticsService subjectStatisticsService,
            BadgeService badgeService) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.childRepository = childRepository;
        this.subjectRepository = subjectRepository;
        this.skillLevelRepository = skillLevelRepository;
        this.aiProviderService = aiProviderService;
        this.quizService = quizService;
        this.progressTrackingService = progressTrackingService;
        this.contentModerationService = contentModerationService;
        this.learningPathService = learningPathService;
        this.mascotService = mascotService;
        this.subjectStatisticsService = subjectStatisticsService;
        this.badgeService = badgeService;
    }
    
    /**
     * Process a chat query with quiz-gating logic
     */
    @Transactional
    public ChatResponse processQuery(ChatRequest request) {
        Child child = childRepository.findById(request.getChildId())
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        
        String query = request.getQuery();

        // Step 1: Content Moderation - Check if query is appropriate
        ModerationResult moderationResult = contentModerationService.moderateContent(query);

        if (moderationResult.isFlagged()) {
            log.warn("Query flagged by moderation for child {}: {}", child.getId(), moderationResult.getReason());

            // Save flagged user message
            ChatMessage flaggedMessage = ChatMessage.builder()
                    .chatSession(session)
                    .role(ChatMessage.MessageRole.USER)
                    .content(query)
                    .contentModeration("FLAGGED: " + moderationResult.getReason())
                    .build();
            chatMessageRepository.save(flaggedMessage);

            // Return error response
            String safetyMessage = "I'm sorry, but I can't help with that question. " +
                    "Let's talk about something educational and appropriate instead! " +
                    "What would you like to learn about today?";

            ChatMessage safetyResponse = ChatMessage.builder()
                    .chatSession(session)
                    .role(ChatMessage.MessageRole.ASSISTANT)
                    .content(safetyMessage)
                    .build();
            chatMessageRepository.save(safetyResponse);

            return ChatResponse.builder()
                    .message(safetyMessage)
                    .responseType(ChatResponse.ResponseType.FULL_ANSWER)
                    .build();
        }

        // Save approved user message
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .role(ChatMessage.MessageRole.USER)
                .content(query)
                .contentModeration("APPROVED")
                .build();
        chatMessageRepository.save(userMessage);

        // OPTIMIZATION: Check if there's an active learning path for this exact query
        // This handles the "Retake Quiz" scenario - reuse the same quiz!
        Optional<com.thinkfirst.model.LearningPath> existingPath =
                learningPathService.findActiveLearningPath(query, child.getId());

        if (existingPath.isPresent()) {
            com.thinkfirst.model.LearningPath learningPath = existingPath.get();
            Quiz existingQuiz = learningPath.getQuiz();

            log.info("RETAKE QUIZ: Reusing existing quiz {} for query: {}", existingQuiz.getId(), query);

            // Return the SAME quiz - don't generate new questions or reveal answer
            return ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.QUIZ_REQUIRED)
                    .quiz(existingQuiz)
                    .message("Let's try this quiz again! You've completed the learning journey - show me what you've learned!")
                    .build();

            // Don't save a new chat message - we're just retaking the quiz
            // The answer is already stored in the original ChatMessage
        }

        // Step 1: Determine subject - use session subject if available, otherwise use General
        // OPTIMIZATION: Skip AI subject analysis - we'll let the quiz/answer generation infer subject from query
        Subject subject;
        if (session.getSubject() != null) {
            subject = session.getSubject();
            log.info("Using existing session subject: {}", subject.getName());
        } else {
            // Default to General subject - the AI will handle subject-specific content based on the query
            subject = subjectRepository.findByName("General")
                    .orElseThrow(() -> new RuntimeException("Default subject not found"));
            session.setSubject(subject);
            log.info("Using General subject for new session");
        }
        
        // Step 2: Check if child has prerequisite knowledge
        boolean hasPrerequisite = progressTrackingService.checkPrerequisite(child.getId(), subject.getId());
        
        ChatResponse response;
        
        if (!hasPrerequisite) {
            // Step 3: Generate prerequisite quiz
            Quiz quiz = quizService.generatePrerequisiteQuiz(child.getId(), child.getAge(), subject, query);

            // Record question in subject statistics
            subjectStatisticsService.recordQuestion(child.getId(), subject.getId());

            response = ChatResponse.withQuiz(quiz);

            // Add Quizzy's quiz start message
            response.setMascotMessage(mascotService.getQuizStartMessage(subject));

            // Save assistant message with quiz requirement
            ChatMessage assistantMessage = ChatMessage.builder()
                    .chatSession(session)
                    .role(ChatMessage.MessageRole.ASSISTANT)
                    .content(response.getMessage())
                    .associatedQuiz(quiz)
                    .requiresQuizCompletion(true)
                    .build();
            chatMessageRepository.save(assistantMessage);

        } else {
            // Step 4: Generate AI response (but don't send it yet)
            String aiResponse = aiProviderService.generateEducationalResponse(
                    query, child.getAge(), subject.getName()
            );

            // Step 5: Generate verification quiz
            Quiz verificationQuiz = quizService.generateVerificationQuiz(
                    query, aiResponse, child.getId(), subject
            );

            // DON'T send the answer yet - student must pass verification quiz first
            response = ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.QUIZ_REQUIRED)
                    .quiz(verificationQuiz)
                    .message("I have an answer for you! But first, let me make sure you're ready to understand it. Please complete this quick quiz.")
                    .build();

            // Save assistant message with the answer stored but not shown
            ChatMessage assistantMessage = ChatMessage.builder()
                    .chatSession(session)
                    .role(ChatMessage.MessageRole.ASSISTANT)
                    .content(aiResponse)  // Store the answer for later
                    .associatedQuiz(verificationQuiz)
                    .requiresQuizCompletion(true)  // Changed to true - quiz must be completed
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(assistantMessage);
            response.setMessageId(savedMessage.getId());
        }
        
        // Update session
        session.setMessageCount(session.getMessageCount() + 2);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
        
        // Update child activity
        child.setLastActiveDate(LocalDateTime.now());
        progressTrackingService.updateStreak(child);
        childRepository.save(child);
        
        return response;
    }
    
    /**
     * Process quiz result and provide appropriate response
     */
    @Transactional
    public ChatResponse processQuizResult(Long childId, Long quizId, Integer score) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));

        Quiz quiz = quizService.quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // Find the chat message associated with this quiz to get the stored answer
        ChatMessage messageWithAnswer = chatMessageRepository.findByAssociatedQuizId(quizId)
                .orElse(null);

        ChatResponse response;

        if (score >= 70) {
            // Full answer unlocked - send the stored answer
            String answer = messageWithAnswer != null ? messageWithAnswer.getContent() :
                    "Great job! You've unlocked full answers for this topic.";

            response = ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.FULL_ANSWER)
                    .message(answer)
                    .build();
        } else if (score >= 40) {
            // Partial hint - generate a hint based on the topic
            String hint = messageWithAnswer != null ?
                    aiProviderService.generateHint(
                            messageWithAnswer.getContent(),
                            quiz.getSubject().getName(),
                            child.getAge()
                    ) :
                    "You're making progress! Here's a hint to help you: Think about the key concepts we just covered.";
            response = ChatResponse.withHint(hint);
        } else {
            // Guided questions - generate questions to help student learn
            String guidedQuestions = messageWithAnswer != null ?
                    aiProviderService.generateEducationalResponse(
                            "Generate 2-3 simple guiding questions (without answers) to help a " +
                            child.getAge() + "-year-old think about: " + messageWithAnswer.getContent(),
                            child.getAge(),
                            quiz.getSubject().getName()
                    ) :
                    "Let's work through this together. Try the quiz again, and I'll help guide you!";

            response = ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.GUIDED_QUESTIONS)
                    .message(guidedQuestions)
                    .build();
        }

        return response;
    }
    
    /**
     * Create a new chat session
     */
    @Transactional
    public ChatSession createSession(Long childId, String title) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found with ID: " + childId));

        ChatSession session = ChatSession.builder()
                .child(child)
                .title(title != null ? title : "New Chat")
                .messageCount(0)
                .archived(false)
                .build();
        
        return chatSessionRepository.save(session);
    }
    
    /**
     * Get chat history for a session
     */
    public List<ChatMessage> getChatHistory(Long sessionId) {
        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);
    }
    
    /**
     * Get all sessions for a child
     */
    public List<ChatSession> getChildSessions(Long childId) {
        return chatSessionRepository.findByChildIdOrderByUpdatedAtDesc(childId);
    }
}

