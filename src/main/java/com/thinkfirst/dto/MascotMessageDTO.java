package com.thinkfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Quizzy the Owl mascot messages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MascotMessageDTO {
    private String message;
    private String type;
    private String icon;
    
    /**
     * Create a default Quizzy message
     */
    public static MascotMessageDTO createDefault(String message, String type) {
        return MascotMessageDTO.builder()
                .message(message)
                .type(type)
                .icon("🦉")
                .build();
    }
}

