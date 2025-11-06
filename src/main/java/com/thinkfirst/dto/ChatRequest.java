package com.thinkfirst.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    @NotNull(message = "Child ID is required")
    private Long childId;
    
    @NotNull(message = "Session ID is required")
    private Long sessionId;
    
    @NotBlank(message = "Query cannot be empty")
    private String query;
}

