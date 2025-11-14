package com.thinkfirst.service;

import com.thinkfirst.dto.MascotMessageDTO;
import com.thinkfirst.model.MascotMessage;
import com.thinkfirst.model.Subject;
import com.thinkfirst.repository.MascotMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Service for Quizzy the Owl mascot messages
 */
@Service
public class MascotService {
    
    private static final Logger log = LoggerFactory.getLogger(MascotService.class);
    private final MascotMessageRepository mascotMessageRepository;
    private final Random random = new Random();
    
    public MascotService(MascotMessageRepository mascotMessageRepository) {
        this.mascotMessageRepository = mascotMessageRepository;
    }
    
    /**
     * Get a welcome message for first-time users
     */
    public MascotMessageDTO getWelcomeMessage() {
        return getMessageByType(MascotMessage.MessageType.WELCOME, 
            "Hi! I'm Quizzy the Owl 🦉. I'm here to help you learn and have fun! Ask me anything!");
    }
    
    /**
     * Get a message before starting a quiz
     */
    public MascotMessageDTO getQuizStartMessage(Subject subject) {
        List<MascotMessage> messages = mascotMessageRepository.findByTypeAndSubject(
            MascotMessage.MessageType.QUIZ_START, subject
        );
        
        if (!messages.isEmpty()) {
            return toDTO(messages.get(random.nextInt(messages.size())));
        }
        
        return MascotMessageDTO.createDefault(
            "Let's test your knowledge! Take your time and think carefully. You've got this! 🦉",
            "QUIZ_START"
        );
    }
    
    /**
     * Get a message after quiz completion based on score
     */
    public MascotMessageDTO getQuizResultMessage(int score, Subject subject) {
        MascotMessage.MessageType type;
        String defaultMessage;
        
        if (score >= 70) {
            type = MascotMessage.MessageType.QUIZ_SUCCESS;
            defaultMessage = "Excellent work! You scored " + score + "%! I knew you could do it! 🎉";
        } else if (score >= 40) {
            type = MascotMessage.MessageType.QUIZ_PARTIAL;
            defaultMessage = "Good effort! You scored " + score + "%. Let me give you some hints to help you improve! 💡";
        } else {
            type = MascotMessage.MessageType.QUIZ_FAIL;
            defaultMessage = "Don't worry! Learning takes practice. Let's review some lessons together! 📚";
        }
        
        List<MascotMessage> messages = mascotMessageRepository.findByTypeAndScore(type, score);
        
        if (!messages.isEmpty()) {
            return toDTO(messages.get(random.nextInt(messages.size())));
        }
        
        return MascotMessageDTO.createDefault(defaultMessage, type.name());
    }
    
    /**
     * Get an encouragement message
     */
    public MascotMessageDTO getEncouragementMessage() {
        return getMessageByType(MascotMessage.MessageType.ENCOURAGEMENT,
            "Keep up the great work! Every question you ask makes you smarter! 🌟");
    }
    
    /**
     * Get a message when providing a hint
     */
    public MascotMessageDTO getHintMessage() {
        return getMessageByType(MascotMessage.MessageType.HINT_GIVEN,
            "Here's a hint to help you think about it differently! 💡");
    }
    
    /**
     * Get a message for achievement unlock
     */
    public MascotMessageDTO getAchievementMessage(String badgeName) {
        List<MascotMessage> messages = mascotMessageRepository.findByTypeAndActiveTrue(
            MascotMessage.MessageType.ACHIEVEMENT_UNLOCK
        );
        
        if (!messages.isEmpty()) {
            MascotMessage message = messages.get(random.nextInt(messages.size()));
            String customMessage = message.getMessage().replace("{badge}", badgeName);
            return MascotMessageDTO.builder()
                    .message(customMessage)
                    .type(message.getType().name())
                    .icon("🦉")
                    .build();
        }
        
        return MascotMessageDTO.createDefault(
            "Congratulations! You've earned the '" + badgeName + "' badge! 🏆",
            "ACHIEVEMENT_UNLOCK"
        );
    }
    
    /**
     * Get a subject-specific help message
     */
    public MascotMessageDTO getSubjectHelpMessage(Subject subject) {
        MascotMessage.MessageContext context = getContextFromSubject(subject);
        
        List<MascotMessage> messages = mascotMessageRepository.findByTypeAndContextAndActiveTrue(
            MascotMessage.MessageType.SUBJECT_HELP, context
        );
        
        if (!messages.isEmpty()) {
            return toDTO(messages.get(random.nextInt(messages.size())));
        }
        
        return MascotMessageDTO.createDefault(
            "I noticed you're learning about " + subject.getName() + ". Great choice! Let me help you! 🦉",
            "SUBJECT_HELP"
        );
    }
    
    private MascotMessageDTO getMessageByType(MascotMessage.MessageType type, String defaultMessage) {
        List<MascotMessage> messages = mascotMessageRepository.findByTypeAndActiveTrue(type);
        
        if (!messages.isEmpty()) {
            return toDTO(messages.get(random.nextInt(messages.size())));
        }
        
        return MascotMessageDTO.createDefault(defaultMessage, type.name());
    }
    
    private MascotMessageDTO toDTO(MascotMessage message) {
        return MascotMessageDTO.builder()
                .message(message.getMessage())
                .type(message.getType().name())
                .icon("🦉")
                .build();
    }
    
    private MascotMessage.MessageContext getContextFromSubject(Subject subject) {
        return switch (subject.getName().toUpperCase()) {
            case "MATHEMATICS" -> MascotMessage.MessageContext.MATHEMATICS;
            case "SCIENCE" -> MascotMessage.MessageContext.SCIENCE;
            case "HISTORY" -> MascotMessage.MessageContext.HISTORY;
            case "ENGLISH" -> MascotMessage.MessageContext.ENGLISH;
            case "GEOGRAPHY" -> MascotMessage.MessageContext.GEOGRAPHY;
            case "COMPUTER SCIENCE" -> MascotMessage.MessageContext.COMPUTER_SCIENCE;
            case "ART" -> MascotMessage.MessageContext.ART;
            case "MUSIC" -> MascotMessage.MessageContext.MUSIC;
            default -> MascotMessage.MessageContext.GENERAL;
        };
    }
}

