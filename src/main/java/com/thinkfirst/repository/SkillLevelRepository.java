package com.thinkfirst.repository;

import com.thinkfirst.model.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillLevelRepository extends JpaRepository<SkillLevel, Long> {
    Optional<SkillLevel> findByChildIdAndSubjectId(Long childId, Long subjectId);
    List<SkillLevel> findByChildId(Long childId);
    List<SkillLevel> findBySubjectId(Long subjectId);
}

