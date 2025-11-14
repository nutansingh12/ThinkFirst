package com.thinkfirst.repository;

import com.thinkfirst.model.Badge;
import com.thinkfirst.model.Child;
import com.thinkfirst.model.ChildBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChildBadgeRepository extends JpaRepository<ChildBadge, Long> {
    
    /**
     * Find all badges earned by a child
     */
    List<ChildBadge> findByChildOrderByEarnedAtDesc(Child child);
    
    /**
     * Check if child has earned a specific badge
     */
    Optional<ChildBadge> findByChildAndBadge(Child child, Badge badge);
    
    /**
     * Find unseen badge notifications for a child
     */
    List<ChildBadge> findByChildAndNotificationSeenFalseOrderByEarnedAtDesc(Child child);
    
    /**
     * Count total badges earned by a child
     */
    long countByChild(Child child);
    
    /**
     * Check if child has earned a badge
     */
    boolean existsByChildAndBadge(Child child, Badge badge);
}

