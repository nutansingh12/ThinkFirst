package com.thinkfirst.repository;

import com.thinkfirst.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByChildIdOrderByEarnedAtDesc(Long childId);
    List<Achievement> findByChildIdAndType(Long childId, Achievement.AchievementType type);
}

