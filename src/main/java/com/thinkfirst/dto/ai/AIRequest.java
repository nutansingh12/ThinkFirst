package com.thinkfirst.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {
    private String prompt;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private String systemPrompt;
}

