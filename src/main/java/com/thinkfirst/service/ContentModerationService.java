package com.thinkfirst.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.config.AIProviderConfig;
import com.thinkfirst.dto.ModerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Content Moderation Service using OpenAI Moderation API
 * Filters inappropriate content before processing queries
 */
@Service
public class ContentModerationService {

    private static final Logger log = LoggerFactory.getLogger(ContentModerationService.class);
    
    private final WebClient webClient;
    private final AIProviderConfig config;
    private final ObjectMapper objectMapper;
    private final boolean moderationEnabled;
    
    public ContentModerationService(
            WebClient.Builder webClientBuilder, 
            AIProviderConfig config,
            ObjectMapper objectMapper,
            @Value("${app.content.moderation-enabled:true}") boolean moderationEnabled) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.moderationEnabled = moderationEnabled;
        
        // Use OpenAI API for moderation
        this.webClient = webClientBuilder
                .baseUrl(config.getOpenai().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getOpenai().getApiKey())
                .build();
    }
    
    /**
     * Moderate content using OpenAI Moderation API
     * @param content The text content to moderate
     * @return ModerationResult with flagged status and categories
     */
    public ModerationResult moderateContent(String content) {
        if (!moderationEnabled) {
            log.debug("Content moderation is disabled, skipping moderation");
            return ModerationResult.approved();
        }
        
        if (!config.getOpenai().isEnabled() || 
            config.getOpenai().getApiKey() == null || 
            config.getOpenai().getApiKey().isEmpty()) {
            log.warn("OpenAI API not configured, skipping moderation");
            return ModerationResult.approved();
        }
        
        try {
            Map<String, Object> requestBody = Map.of("input", content);
            
            String response = webClient.post()
                    .uri("/moderations")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("OpenAI Moderation API client error: {}", body);
                                        return Mono.error(new RuntimeException("Moderation API error: " + body));
                                    }))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("OpenAI Moderation API server error: {}", body);
                                        return Mono.error(new RuntimeException("Moderation API error: " + body));
                                    }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            return parseModerationResponse(response);
            
        } catch (Exception e) {
            log.error("Error calling OpenAI Moderation API, defaulting to APPROVED", e);
            // Fail open - if moderation service is down, allow content through
            // but log the error for monitoring
            return ModerationResult.approved();
        }
    }
    
    /**
     * Parse OpenAI Moderation API response
     */
    private ModerationResult parseModerationResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("results").get(0);
            
            boolean flagged = result.path("flagged").asBoolean(false);
            
            if (!flagged) {
                return ModerationResult.approved();
            }
            
            // Extract flagged categories
            JsonNode categories = result.path("categories");
            JsonNode categoryScores = result.path("category_scores");
            
            ModerationResult.ModerationResultBuilder builder = ModerationResult.builder()
                    .flagged(true);
            
            // Check specific categories
            if (categories.path("sexual").asBoolean(false)) {
                builder.reason("Sexual content detected");
                builder.category("sexual");
            } else if (categories.path("hate").asBoolean(false)) {
                builder.reason("Hate speech detected");
                builder.category("hate");
            } else if (categories.path("harassment").asBoolean(false)) {
                builder.reason("Harassment detected");
                builder.category("harassment");
            } else if (categories.path("self-harm").asBoolean(false)) {
                builder.reason("Self-harm content detected");
                builder.category("self-harm");
            } else if (categories.path("sexual/minors").asBoolean(false)) {
                builder.reason("Inappropriate content involving minors detected");
                builder.category("sexual/minors");
            } else if (categories.path("hate/threatening").asBoolean(false)) {
                builder.reason("Threatening hate speech detected");
                builder.category("hate/threatening");
            } else if (categories.path("violence/graphic").asBoolean(false)) {
                builder.reason("Graphic violence detected");
                builder.category("violence/graphic");
            } else if (categories.path("self-harm/intent").asBoolean(false)) {
                builder.reason("Self-harm intent detected");
                builder.category("self-harm/intent");
            } else if (categories.path("self-harm/instructions").asBoolean(false)) {
                builder.reason("Self-harm instructions detected");
                builder.category("self-harm/instructions");
            } else if (categories.path("harassment/threatening").asBoolean(false)) {
                builder.reason("Threatening harassment detected");
                builder.category("harassment/threatening");
            } else if (categories.path("violence").asBoolean(false)) {
                builder.reason("Violent content detected");
                builder.category("violence");
            } else {
                builder.reason("Inappropriate content detected");
                builder.category("other");
            }
            
            log.warn("Content flagged by moderation: {}", builder.build().getReason());
            return builder.build();
            
        } catch (Exception e) {
            log.error("Error parsing moderation response", e);
            return ModerationResult.approved();
        }
    }
    
    /**
     * Check if content is safe for children
     * @param content The text to check
     * @return true if content is safe, false if flagged
     */
    public boolean isSafeForChildren(String content) {
        ModerationResult result = moderateContent(content);
        return !result.isFlagged();
    }
    
    /**
     * Get detailed moderation status
     * @param content The text to check
     * @return "APPROVED" or "FLAGGED: reason"
     */
    public String getModerationStatus(String content) {
        ModerationResult result = moderateContent(content);
        if (result.isFlagged()) {
            return "FLAGGED: " + result.getReason();
        }
        return "APPROVED";
    }
}

