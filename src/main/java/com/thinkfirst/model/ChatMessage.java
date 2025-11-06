package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * Individual chat message in a session
 */
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    @JsonIgnore
    private ChatSession chatSession;
    
    @Enumerated(EnumType.STRING)
    private MessageRole role; // USER, ASSISTANT
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz associatedQuiz; // If this message triggered a quiz
    
    private Boolean requiresQuizCompletion = false;
    
    private String contentModeration; // APPROVED, FLAGGED, BLOCKED
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum MessageRole {
        USER, ASSISTANT
    }
}

