package com.thinkfirst.repository;

import com.thinkfirst.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByChildId(Long childId);
    List<QuizAttempt> findByChildIdAndQuizId(Long childId, Long quizId);
    
    @Query("SELECT AVG(qa.score) FROM QuizAttempt qa WHERE qa.child.id = :childId")
    Double getAverageScoreByChildId(Long childId);
    
    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.child.id = :childId AND qa.passed = true")
    Long countPassedQuizzesByChildId(Long childId);
}

