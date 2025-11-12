package com.thinkfirst.repository;

import com.thinkfirst.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);
    Optional<ChatMessage> findByAssociatedQuizId(Long quizId);

    /**
     * Find the USER message that came before the ASSISTANT message with the quiz
     * This gets the original question, not the AI-generated answer
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chatSession.id = " +
           "(SELECT am.chatSession.id FROM ChatMessage am WHERE am.associatedQuiz.id = :quizId) " +
           "AND m.role = 'USER' " +
           "AND m.createdAt < (SELECT am.createdAt FROM ChatMessage am WHERE am.associatedQuiz.id = :quizId) " +
           "ORDER BY m.createdAt DESC")
    Optional<ChatMessage> findUserMessageBeforeQuiz(@Param("quizId") Long quizId);
}

