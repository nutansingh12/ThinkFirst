package com.thinkfirst.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.config.AIProviderConfig;
import com.thinkfirst.exception.AIProviderException;
import com.thinkfirst.exception.RateLimitException;
import com.thinkfirst.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek AI Provider Service
 * Cost: $0.28/1M input tokens, $0.42/1M output tokens (5-10x cheaper than OpenAI)
 */
@Service
public class DeepSeekService implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);
    
    private final WebClient webClient;
    private final AIProviderConfig config;
    private final ObjectMapper objectMapper;
    private String currentModel;
    
    public DeepSeekService(WebClient.Builder webClientBuilder, AIProviderConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.currentModel = config.getDeepseek() != null && config.getDeepseek().getModels() != null
                ? config.getDeepseek().getModels().get("default")
                : "deepseek-chat";
        
        this.webClient = webClientBuilder
                .baseUrl(config.getDeepseek() != null ? config.getDeepseek().getBaseUrl() : "https://api.deepseek.com/v1")
                .build();
    }
    
    @Override
    public String getProviderName() {
        return "DeepSeek";
    }
    
    @Override
    public boolean isAvailable() {
        return config.getDeepseek() != null
                && config.getDeepseek().isEnabled() 
                && config.getDeepseek().getApiKey() != null 
                && !config.getDeepseek().getApiKey().isEmpty();
    }
    
    @Override
    public String generateEducationalResponse(String query, int age, String subject) {
        if (!isAvailable()) {
            throw new AIProviderException("DeepSeek", "DeepSeek API is not available or not configured");
        }

        String systemPrompt = String.format(
            "You are an educational AI tutor for children aged %d. " +
            "Provide clear, age-appropriate explanations about %s. " +
            "Use simple language, examples, and encourage critical thinking. " +
            "Keep responses concise (under 200 words).",
            age, subject
        );

        return callDeepSeekAPI(systemPrompt, query, currentModel, null);
    }

    @Override
    public String generateLearningLessons(String prompt, int age, String subject) {
        if (!isAvailable()) {
            throw new AIProviderException("DeepSeek", "DeepSeek API is not available or not configured");
        }

        String systemPrompt = String.format(
            "You are an educational AI tutor for children aged %d. " +
            "Generate detailed, comprehensive learning lessons about %s. " +
            "Use clear, age-appropriate language with examples and real-world applications. " +
            "Return ONLY valid JSON - no markdown, no code blocks, no extra text.",
            age, subject
        );

        // Use higher token limit for detailed lessons (8000 tokens)
        return callDeepSeekAPI(systemPrompt, prompt, currentModel, 8000);
    }

    @Override
    public List<Question> generateQuestions(String topic, String subject, int count, String difficulty, Integer age) {
        if (!isAvailable()) {
            throw new AIProviderException("DeepSeek", "DeepSeek API is not available or not configured");
        }
        
        String systemPrompt = "You are an educational quiz generator. Generate questions in valid JSON format only.";
        
        String userPrompt = String.format(
            "Generate %d multiple-choice questions about '%s' in the subject of %s at %s difficulty level, for age %d. " +
            "Return ONLY a valid JSON array with this exact structure (no markdown, no code blocks):\n" +
            "[{\"question\":\"What is 2+2?\",\"options\":[\"3\",\"4\",\"5\",\"6\"],\"correctIndex\":1,\"explanation\":\"2+2 equals 4\"}]\n" +
            "Each question must have 4 different answer options with actual text (not just A,B,C,D). " +
            "Make questions educational and age-appropriate.",
            count, topic, subject, difficulty, age
        );
        
        String response = callDeepSeekAPI(systemPrompt, userPrompt, currentModel, null);
        return parseQuestionsFromJSON(response);
    }

    @Override
    public String generateHint(String query, String subject, int age) {
        if (!isAvailable()) {
            throw new AIProviderException("DeepSeek", "DeepSeek API is not available or not configured");
        }

        String systemPrompt = "You are a helpful educational assistant that provides hints without giving away answers.";

        String userPrompt = String.format(
            "For a %d-year-old learning about %s, provide a helpful hint (not the full answer) for: %s\n" +
            "The hint should guide their thinking without giving away the answer. Keep it under 50 words.",
            age, subject, query
        );

        return callDeepSeekAPI(systemPrompt, userPrompt, currentModel, null);
    }

    @Override
    public String analyzeQuerySubject(String query) {
        if (!isAvailable()) {
            throw new AIProviderException("DeepSeek", "DeepSeek API is not available or not configured");
        }

        String systemPrompt = "You are a subject classifier. Return only the subject name, nothing else.";

        String userPrompt = String.format(
            "Analyze this question and return ONLY the subject category (one word): %s\n" +
            "Choose from: Mathematics, Science, English, History, Geography, Computer Science, Art, Music, General",
            query
        );

        String response = callDeepSeekAPI(systemPrompt, userPrompt, currentModel, null);
        return response.trim().split("\\s+")[0]; // Get first word
    }

    /**
     * Call DeepSeek API with chat completion
     * DeepSeek uses OpenAI-compatible API format
     */
    private String callDeepSeekAPI(String systemPrompt, String userPrompt, String model, Integer maxTokens) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);
            
            requestBody.put("temperature", config.getDeepseek().getTemperature());
            requestBody.put("max_tokens", maxTokens != null ? maxTokens : config.getDeepseek().getMaxTokens());

            log.debug("Calling DeepSeek API with model: {}", model);

            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + config.getDeepseek().getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.value() == 429, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.warn("DeepSeek API rate limit exceeded: {}", body);
                                        return Mono.error(new RateLimitException("DeepSeek", "DeepSeek API rate limit exceeded"));
                                    }))
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("DeepSeek API 4xx error: {}", body);
                                        return Mono.error(new AIProviderException("DeepSeek", "Client error: " + body));
                                    }))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("DeepSeek API 5xx error: {}", body);
                                        return Mono.error(new AIProviderException("DeepSeek", "Server error: " + body));
                                    }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getDeepseek().getTimeoutSeconds()))
                    .block();

            return extractContentFromResponse(response);
            
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling DeepSeek API: {}", e.getMessage(), e);
            throw new AIProviderException("DeepSeek", "Failed to call DeepSeek API: " + e.getMessage(), e);
        }
    }

    /**
     * Extract content from DeepSeek response (OpenAI-compatible format)
     */
    private String extractContentFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Failed to parse DeepSeek response: {}", response, e);
            throw new AIProviderException("DeepSeek", "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Parse questions from JSON response
     */
    private List<Question> parseQuestionsFromJSON(String jsonResponse) {
        try {
            // Remove markdown code blocks if present
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            }

            JsonNode questionsNode = objectMapper.readTree(cleanJson);
            List<Question> questions = new ArrayList<>();

            for (JsonNode questionNode : questionsNode) {
                Question question = Question.builder()
                        .questionText(questionNode.get("question").asText())
                        .type(Question.QuestionType.MULTIPLE_CHOICE)
                        .correctOptionIndex(questionNode.get("correctIndex").asInt())
                        .explanation(questionNode.has("explanation") ? questionNode.get("explanation").asText() : "")
                        .build();

                // Parse options
                List<String> options = new ArrayList<>();
                JsonNode optionsNode = questionNode.get("options");
                for (JsonNode option : optionsNode) {
                    options.add(option.asText());
                }
                question.setOptions(options);

                questions.add(question);
            }

            return questions;
        } catch (Exception e) {
            log.error("Failed to parse questions from JSON: {}", jsonResponse, e);
            throw new AIProviderException("DeepSeek", "Failed to parse questions: " + e.getMessage());
        }
    }
}

