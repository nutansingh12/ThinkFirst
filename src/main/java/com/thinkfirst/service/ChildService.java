package com.thinkfirst.service;

import com.thinkfirst.dto.ChildRequest;
import com.thinkfirst.dto.ChildResponse;
import com.thinkfirst.model.Child;
import com.thinkfirst.model.User;
import com.thinkfirst.repository.ChildRepository;
import com.thinkfirst.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChildService {

    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ChildResponse createChild(ChildRequest request) {
        log.info("Creating child with username: {} for parent ID: {}", request.getUsername(), request.getParentId());

        // Check if username already exists
        if (childRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Verify parent exists
        User parent = userRepository.findById(request.getParentId())
                .orElseThrow(() -> {
                    log.error("Parent not found with ID: {}. Available parent IDs: {}",
                        request.getParentId(),
                        userRepository.findAll().stream().map(User::getId).toList());
                    return new RuntimeException("Parent not found with ID: " + request.getParentId());
                });

        // Parse grade level
        Child.GradeLevel gradeLevel = null;
        if (request.getGradeLevel() != null && !request.getGradeLevel().isEmpty()) {
            try {
                gradeLevel = Child.GradeLevel.valueOf(request.getGradeLevel().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to null if invalid
            }
        }

        Child child = Child.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .age(request.getAge())
                .gradeLevel(gradeLevel)
                .parent(parent)
                .currentStreak(0)
                .totalQuestionsAnswered(0)
                .totalQuizzesCompleted(0)
                .active(true)
                .build();

        child = childRepository.save(child);
        return mapToResponse(child);
    }

    public List<ChildResponse> getParentChildren(Long parentId) {
        return childRepository.findByParentId(parentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ChildResponse> getAllChildren() {
        return childRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ChildResponse getChild(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        return mapToResponse(child);
    }

    @Transactional
    public ChildResponse updateChild(Long childId, ChildRequest request) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));

        // Update fields
        if (request.getUsername() != null && !request.getUsername().equals(child.getUsername())) {
            if (childRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            child.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            child.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getAge() != null) {
            child.setAge(request.getAge());
        }

        if (request.getGradeLevel() != null && !request.getGradeLevel().isEmpty()) {
            try {
                child.setGradeLevel(Child.GradeLevel.valueOf(request.getGradeLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid grade level
            }
        }

        child = childRepository.save(child);
        return mapToResponse(child);
    }

    @Transactional
    public void deleteChild(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        childRepository.delete(child);
    }

    private ChildResponse mapToResponse(Child child) {
        return ChildResponse.builder()
                .id(child.getId())
                .username(child.getUsername())
                .age(child.getAge())
                .gradeLevel(child.getGradeLevel() != null ? child.getGradeLevel().name() : null)
                .parentId(child.getParent().getId())
                .currentStreak(child.getCurrentStreak())
                .totalQuestionsAnswered(child.getTotalQuestionsAnswered())
                .totalQuizzesCompleted(child.getTotalQuizzesCompleted())
                .lastActiveDate(child.getLastActiveDate())
                .active(child.getActive())
                .createdAt(child.getCreatedAt())
                .build();
    }
}

