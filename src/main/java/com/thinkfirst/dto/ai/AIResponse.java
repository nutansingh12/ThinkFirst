package com.thinkfirst.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    private String content;
    private String provider;
    private String model;
    private Integer tokensUsed;
}

