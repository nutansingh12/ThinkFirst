package com.thinkfirst.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.config.AIProviderConfig;
import com.thinkfirst.exception.AIProviderException;
import com.thinkfirst.exception.RateLimitException;
import com.thinkfirst.model.Question;
import com.thinkfirst.model.Question.QuestionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    private final WebClient webClient;
    private final AIProviderConfig config;
    private final ObjectMapper objectMapper;
    private final PromptOptimizer promptOptimizer;
    
    public GeminiService(WebClient.Builder webClientBuilder, AIProviderConfig config, ObjectMapper objectMapper, PromptOptimizer promptOptimizer) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.promptOptimizer = promptOptimizer;
        this.webClient = webClientBuilder
                .baseUrl(config.getGemini().getBaseUrl())
                .build();

        // Log configuration on startup
        log.info("Gemini Service initialized - Enabled: {}, API Key present: {}, Base URL: {}",
                config.getGemini().isEnabled(),
                config.getGemini().getApiKey() != null && !config.getGemini().getApiKey().isEmpty(),
                config.getGemini().getBaseUrl());
    }
    
    @Override
    public String getProviderName() {
        return "Gemini";
    }
    
    @Override
    public boolean isAvailable() {
        return config.getGemini().isEnabled() 
                && config.getGemini().getApiKey() != null 
                && !config.getGemini().getApiKey().isEmpty();
    }
    
    @Override
    public String generateEducationalResponse(String query, int age, String subject) {
        if (!isAvailable()) {
            throw new AIProviderException("Gemini", "Gemini API is not available or not configured");
        }

        // Use optimized prompt (67% token reduction)
        String prompt = promptOptimizer.buildEducationalPrompt(query, age, subject);

        return callGeminiAPI(prompt, config.getGemini().getModels().get("default"));
    }
    
    @Override
    public List<Question> generateQuestions(String topic, String subject, int count, String difficulty, Integer age) {
        if (!isAvailable()) {
            throw new AIProviderException("Gemini", "Gemini API is not available or not configured");
        }

        // Use optimized prompt (60% token reduction)
        String prompt = promptOptimizer.buildQuizPrompt(topic, subject, count, difficulty, age);
        
        String response = callGeminiAPI(prompt, config.getGemini().getModels().get("default"));
        return parseQuestionsFromJSON(response);
    }
    
    @Override
    public String generateHint(String query, String subject, int age) {
        if (!isAvailable()) {
            throw new AIProviderException("Gemini", "Gemini API is not available or not configured");
        }

        // Use optimized prompt (70% token reduction)
        String prompt = promptOptimizer.buildHintPrompt(query, age, subject);

        return callGeminiAPI(prompt, config.getGemini().getModels().get("default"));
    }

    @Override
    public String analyzeQuerySubject(String query) {
        if (!isAvailable()) {
            throw new AIProviderException("Gemini", "Gemini API is not available or not configured");
        }

        // Use optimized prompt (75% token reduction)
        String prompt = promptOptimizer.buildSubjectPrompt(query);

        String response = callGeminiAPI(prompt, config.getGemini().getModels().get("default"));
        return response.trim().split("\\s+")[0]; // Get first word
    }
    
    private String callGeminiAPI(String prompt, String model) {
        try {
            String apiKey = config.getGemini().getApiKey();
            log.debug("Calling Gemini API with model: {}, API key length: {}", model, apiKey != null ? apiKey.length() : 0);

            // Gemini API request format
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                ),
                "generationConfig", Map.of(
                    "temperature", config.getGemini().getTemperature(),
                    "maxOutputTokens", config.getGemini().getMaxTokens()
                )
            );

            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        if (clientResponse.statusCode().value() == 429) {
                            return Mono.error(new RateLimitException("Gemini", "Rate limit exceeded"));
                        }
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Gemini API 4xx error: {}", body);
                                    return Mono.error(new AIProviderException("Gemini", "Client error: " + body));
                                });
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("Gemini API 5xx error: {}", body);
                                        return Mono.error(new AIProviderException("Gemini", "Server error: " + body));
                                    }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getGemini().getTimeoutSeconds()))
                    .block();

            return extractTextFromGeminiResponse(response);
            
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new AIProviderException("Gemini", "Failed to call Gemini API: " + e.getMessage(), e);
        }
    }
    
    private String extractTextFromGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
            throw new AIProviderException("Gemini", "Unexpected response format");
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            throw new AIProviderException("Gemini", "Failed to parse response", e);
        }
    }
    
    private List<Question> parseQuestionsFromJSON(String jsonResponse) {
        List<Question> questions = new ArrayList<>();
        try {
            // Clean up response - remove markdown code blocks if present
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }
            
            JsonNode questionsArray = objectMapper.readTree(cleanJson);
            
            for (JsonNode questionNode : questionsArray) {
                Question question = new Question();
                question.setQuestionText(questionNode.path("question").asText());
                question.setType(QuestionType.MULTIPLE_CHOICE);
                
                // Parse options
                List<String> options = new ArrayList<>();
                JsonNode optionsNode = questionNode.path("options");
                if (optionsNode.isArray()) {
                    optionsNode.forEach(opt -> options.add(opt.asText()));
                }
                question.setOptions(options);
                
                question.setCorrectOptionIndex(questionNode.path("correctIndex").asInt());
                question.setExplanation(questionNode.path("explanation").asText());
                
                questions.add(question);
            }
            
        } catch (Exception e) {
            log.error("Error parsing questions from JSON: {}", e.getMessage());
            throw new AIProviderException("Gemini", "Failed to parse questions from response", e);
        }
        
        return questions;
    }
}

