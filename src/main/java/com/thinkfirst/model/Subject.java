package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Subject/Topic entity (Math, Science, English, etc.)
 */
@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name; // "Mathematics", "Science", "English"
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private AgeGroup ageGroup;
    
    @ManyToMany
    @JoinTable(
        name = "subject_prerequisites",
        joinColumns = @JoinColumn(name = "subject_id"),
        inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    private List<Subject> prerequisites = new ArrayList<>();
    
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
    private List<Quiz> quizzes = new ArrayList<>();
    
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
    private List<SkillLevel> skillLevels = new ArrayList<>();
    
    private Boolean active = true;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum AgeGroup {
        ELEMENTARY, MIDDLE_SCHOOL, HIGH_SCHOOL, GENERAL
    }
}

