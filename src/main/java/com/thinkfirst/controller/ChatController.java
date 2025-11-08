package com.thinkfirst.controller;

import com.thinkfirst.dto.ChatRequest;
import com.thinkfirst.dto.ChatResponse;
import com.thinkfirst.model.ChatMessage;
import com.thinkfirst.model.ChatSession;
import com.thinkfirst.service.ChatService;
import com.thinkfirst.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "AI chat endpoints with quiz-gating")
public class ChatController {

    private final ChatService chatService;
    private final RateLimitService rateLimitService;

    public ChatController(ChatService chatService, RateLimitService rateLimitService) {
        this.chatService = chatService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/query")
    @Operation(summary = "Send a chat query (quiz-gated)")
    public ResponseEntity<ChatResponse> sendQuery(@Valid @RequestBody ChatRequest request) {
        // Check rate limits
        rateLimitService.checkChatRateLimit(request.getChildId());
        rateLimitService.checkDailyQuestionLimit(request.getChildId());

        return ResponseEntity.ok(chatService.processQuery(request));
    }
    
    @PostMapping("/session")
    @Operation(summary = "Create a new chat session")
    public ResponseEntity<ChatSession> createSession(
            @RequestParam Long childId,
            @RequestParam(required = false) String title) {
        return ResponseEntity.ok(chatService.createSession(childId, title));
    }
    
    @GetMapping("/session/{sessionId}/history")
    @Operation(summary = "Get chat history for a session")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getChatHistory(sessionId));
    }
    
    @GetMapping("/child/{childId}/sessions")
    @Operation(summary = "Get all sessions for a child")
    public ResponseEntity<List<ChatSession>> getChildSessions(@PathVariable Long childId) {
        return ResponseEntity.ok(chatService.getChildSessions(childId));
    }
}

