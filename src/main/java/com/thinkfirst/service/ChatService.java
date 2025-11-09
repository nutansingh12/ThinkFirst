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

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            ChildRepository childRepository,
            SubjectRepository subjectRepository,
            SkillLevelRepository skillLevelRepository,
            com.thinkfirst.service.ai.AIProviderService aiProviderService,
            QuizService quizService,
            ProgressTrackingService progressTrackingService,
            ContentModerationService contentModerationService) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.childRepository = childRepository;
        this.subjectRepository = subjectRepository;
        this.skillLevelRepository = skillLevelRepository;
        this.aiProviderService = aiProviderService;
        this.quizService = quizService;
        this.progressTrackingService = progressTrackingService;
        this.contentModerationService = contentModerationService;
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
        
        // Step 1: Analyze query to determine subject using AI provider
        String subjectName = aiProviderService.analyzeQuerySubject(query);
        Subject subject = subjectRepository.findByName(subjectName)
                .orElse(subjectRepository.findByName("General")
                        .orElseThrow(() -> new RuntimeException("Default subject not found")));
        
        // Update session subject if not set
        if (session.getSubject() == null) {
            session.setSubject(subject);
        }
        
        // Step 2: Check if child has prerequisite knowledge
        boolean hasPrerequisite = progressTrackingService.checkPrerequisite(child.getId(), subject.getId());
        
        ChatResponse response;
        
        if (!hasPrerequisite) {
            // Step 3: Generate prerequisite quiz
            Quiz quiz = quizService.generatePrerequisiteQuiz(child.getId(), subject);
            
            response = ChatResponse.withQuiz(quiz);
            
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
            // Step 4: Get full AI response using AI provider
            String aiResponse = aiProviderService.generateEducationalResponse(
                    query, child.getAge(), subject.getName()
            );

            // Step 5: Generate verification quiz
            Quiz verificationQuiz = quizService.generateVerificationQuiz(
                    query, aiResponse, child.getId(), subject
            );
            
            response = ChatResponse.withAnswerAndQuiz(aiResponse, verificationQuiz);
            
            // Save assistant message
            ChatMessage assistantMessage = ChatMessage.builder()
                    .chatSession(session)
                    .role(ChatMessage.MessageRole.ASSISTANT)
                    .content(aiResponse)
                    .associatedQuiz(verificationQuiz)
                    .requiresQuizCompletion(false)
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
        
        ChatResponse response;
        
        if (score >= 70) {
            // Full answer unlocked
            response = ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.FULL_ANSWER)
                    .message("Great job! You've unlocked full answers for this topic. What would you like to know?")
                    .build();
        } else if (score >= 40) {
            // Partial hint
            String hint = "You're making progress! Here's a hint to help you: Think about the key concepts we just covered.";
            response = ChatResponse.withHint(hint);
        } else {
            // Guided questions only
            response = ChatResponse.builder()
                    .responseType(ChatResponse.ResponseType.GUIDED_QUESTIONS)
                    .message("Let's work through this together. What do you already know about this topic?")
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

