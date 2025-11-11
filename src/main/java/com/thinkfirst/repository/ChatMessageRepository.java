package com.thinkfirst.repository;

import com.thinkfirst.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);
    Optional<ChatMessage> findByAssociatedQuizId(Long quizId);
}

