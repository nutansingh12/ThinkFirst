package com.thinkfirst.repository;

import com.thinkfirst.model.MascotMessage;
import com.thinkfirst.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MascotMessageRepository extends JpaRepository<MascotMessage, Long> {
    
    /**
     * Find active messages by type
     */
    List<MascotMessage> findByTypeAndActiveTrue(MascotMessage.MessageType type);
    
    /**
     * Find active messages by type and context
     */
    List<MascotMessage> findByTypeAndContextAndActiveTrue(
        MascotMessage.MessageType type, 
        MascotMessage.MessageContext context
    );
    
    /**
     * Find messages for quiz results based on score
     */
    @Query("SELECT m FROM MascotMessage m WHERE m.type = :type AND m.active = true " +
           "AND (m.minScore IS NULL OR m.minScore <= :score) " +
           "AND (m.maxScore IS NULL OR m.maxScore >= :score) " +
           "ORDER BY m.priority DESC")
    List<MascotMessage> findByTypeAndScore(
        @Param("type") MascotMessage.MessageType type,
        @Param("score") Integer score
    );
    
    /**
     * Find messages by type and subject
     */
    @Query("SELECT m FROM MascotMessage m WHERE m.type = :type AND m.active = true " +
           "AND (m.subject IS NULL OR m.subject = :subject) " +
           "ORDER BY m.priority DESC")
    List<MascotMessage> findByTypeAndSubject(
        @Param("type") MascotMessage.MessageType type,
        @Param("subject") Subject subject
    );
}

