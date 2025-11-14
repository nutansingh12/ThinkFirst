package com.thinkfirst.repository;

import com.thinkfirst.model.Badge;
import com.thinkfirst.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {
    
    /**
     * Find badge by code
     */
    Optional<Badge> findByCode(String code);
    
    /**
     * Find all active badges
     */
    List<Badge> findByActiveTrueOrderByDisplayOrder();
    
    /**
     * Find badges by category
     */
    List<Badge> findByCategoryAndActiveTrueOrderByDisplayOrder(Badge.BadgeCategory category);
    
    /**
     * Find badges by subject
     */
    List<Badge> findBySubjectAndActiveTrueOrderByDisplayOrder(Subject subject);
    
    /**
     * Find badges by criteria type
     */
    List<Badge> findByCriteriaTypeAndActiveTrue(Badge.CriteriaType criteriaType);
}

