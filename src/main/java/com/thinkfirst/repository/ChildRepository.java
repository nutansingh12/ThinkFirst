package com.thinkfirst.repository;

import com.thinkfirst.model.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChildRepository extends JpaRepository<Child, Long> {
    Optional<Child> findByUsername(String username);
    List<Child> findByParentId(Long parentId);
    Boolean existsByUsername(String username);
}

