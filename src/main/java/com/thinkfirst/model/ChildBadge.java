package com.thinkfirst.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks which badges each child has earned
 */
@Entity
@Table(name = "child_badges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"child_id", "badge_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChildBadge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;
    
    /**
     * When the badge was earned
     */
    @Column(nullable = false)
    private LocalDateTime earnedAt;
    
    /**
     * Whether the child has seen the unlock notification
     */
    @Column(nullable = false)
    private Boolean notificationSeen;
    
    @PrePersist
    protected void onCreate() {
        if (earnedAt == null) {
            earnedAt = LocalDateTime.now();
        }
        if (notificationSeen == null) {
            notificationSeen = false;
        }
    }
}

