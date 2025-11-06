package com.thinkfirst.repository;

import com.thinkfirst.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findBySubjectId(Long subjectId);
    List<Quiz> findBySubjectIdAndDifficulty(Long subjectId, Quiz.DifficultyLevel difficulty);
    List<Quiz> findByType(Quiz.QuizType type);
}

