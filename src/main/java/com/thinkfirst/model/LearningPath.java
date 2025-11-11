package com.thinkfirst.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a structured learning path for students who need to learn prerequisites
 */
@Entity
@Table(name = "learning_paths")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    @JsonIgnore
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnore
    private Quiz quiz;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String originalQuery;

    @Column(columnDefinition = "TEXT")
    private String motivationalMessage;

    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();

    @Column(nullable = false)
    private Integer totalLessons;

    @Column(nullable = false)
    @Builder.Default
    private Integer completedLessons = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return completedLessons >= totalLessons;
    }

    public int getProgressPercentage() {
        if (totalLessons == 0) return 0;
        return (completedLessons * 100) / totalLessons;
    }
}

