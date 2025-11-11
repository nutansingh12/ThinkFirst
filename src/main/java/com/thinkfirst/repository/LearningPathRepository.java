package com.thinkfirst.repository;

import com.thinkfirst.model.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    
    Optional<LearningPath> findByQuizIdAndChildId(Long quizId, Long childId);
    
    Optional<LearningPath> findByIdAndChildId(Long id, Long childId);
}

