package com.thinkfirst.repository;

import com.thinkfirst.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByChildIdAndArchivedFalse(Long childId);
    List<ChatSession> findByChildIdOrderByUpdatedAtDesc(Long childId);
}

