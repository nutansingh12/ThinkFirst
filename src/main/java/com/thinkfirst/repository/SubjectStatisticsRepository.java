package com.thinkfirst.repository;

import com.thinkfirst.model.Child;
import com.thinkfirst.model.Subject;
import com.thinkfirst.model.SubjectStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectStatisticsRepository extends JpaRepository<SubjectStatistics, Long> {
    
    /**
     * Find statistics for a child and subject
     */
    Optional<SubjectStatistics> findByChildAndSubject(Child child, Subject subject);
    
    /**
     * Find all statistics for a child, ordered by question count
     */
    List<SubjectStatistics> findByChildOrderByTotalQuestionsDesc(Child child);
    
    /**
     * Find top subjects for a child (learning profile)
     */
    List<SubjectStatistics> findTop5ByChildOrderByTotalQuestionsDesc(Child child);
}

