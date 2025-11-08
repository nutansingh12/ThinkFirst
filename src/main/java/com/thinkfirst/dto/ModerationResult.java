package com.thinkfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for content moderation results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
    
    private boolean flagged;
    private String reason;
    private String category;
    
    /**
     * Create an approved moderation result
     */
    public static ModerationResult approved() {
        return ModerationResult.builder()
                .flagged(false)
                .reason("Content approved")
                .category("none")
                .build();
    }
    
    /**
     * Create a flagged moderation result
     */
    public static ModerationResult flagged(String reason, String category) {
        return ModerationResult.builder()
                .flagged(true)
                .reason(reason)
                .category(category)
                .build();
    }
}

