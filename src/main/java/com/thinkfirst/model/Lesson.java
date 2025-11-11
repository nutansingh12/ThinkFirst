package com.thinkfirst.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single lesson in a learning path
 */
@Entity
@Table(name = "lessons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    @JsonIgnore
    private LearningPath learningPath;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer displayOrder;

    @ElementCollection
    @CollectionTable(name = "lesson_resources", joinColumns = @JoinColumn(name = "lesson_id"))
    @Builder.Default
    private List<LessonResource> resources = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonResource {
        
        @Enumerated(EnumType.STRING)
        private ResourceType type;
        
        private String title;
        
        private String url;
        
        private String description;
        
        public enum ResourceType {
            VIDEO,
            PRACTICE,
            INTERACTIVE_DEMO,
            READING,
            QUIZ
        }
    }
}

