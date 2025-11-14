package com.thinkfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for achievement badges
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String category;
    private String rarity;
    private String subjectName;
    private Integer criteriaValue;
    private String criteriaType;
    private Boolean earned;
    private LocalDateTime earnedAt;
    private Integer progress;  // Current progress towards earning (0-100%)
    private Boolean isNew;     // True if recently earned and not seen
}

