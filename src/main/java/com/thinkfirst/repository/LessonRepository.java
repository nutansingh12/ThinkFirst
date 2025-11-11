package com.thinkfirst.repository;

import com.thinkfirst.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    
    Optional<Lesson> findByIdAndLearningPathChildId(Long id, Long childId);
}

