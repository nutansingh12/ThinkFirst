package com.thinkfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDTO {
    
    private Long id;
    private String badgeName;
    private String description;
    private String iconUrl;
    private String type;
    private Integer points;
    private LocalDateTime earnedAt;
}

