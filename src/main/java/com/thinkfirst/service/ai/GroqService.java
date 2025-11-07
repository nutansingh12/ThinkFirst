package com.thinkfirst.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.config.AIProviderConfig;
import com.thinkfirst.exception.AIProviderException;
import com.thinkfirst.exception.RateLimitException;
import com.thinkfirst.model.Question;
import com.thinkfirst.model.Question.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GroqService implements AIProvider {
    
    private final WebClient webClient;
    private final AIProviderConfig config;
    private final ObjectMapper objectMapper;
    
    public GroqService(WebClient.Builder webClientBuilder, AIProviderConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(config.getGroq().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getGroq().getApiKey())
                .build();
    }
    
    @Override
    public String getProviderName() {
        return "Groq";
    }
    
    @Override
    public boolean isAvailable() {
        return config.getGroq().isEnabled() 
                && config.getGroq().getApiKey() != null 
                && !config.getGroq().getApiKey().isEmpty();
    }
    
    @Override
    public String generateEducationalResponse(String query, int age, String subject) {
        if (!isAvailable()) {
            throw new AIProviderException("Groq", "Groq API is not available or not configured");
        }
        
        String systemPrompt = String.format(
            "You are an educational AI tutor for children aged %d. " +
            "Provide clear, age-appropriate explanations about %s. " +
            "Use simple language, examples, and encourage critical thinking. " +
            "Keep responses concise (under 200 words).",
            age, subject
        );
        
        return callGroqAPI(systemPrompt, query, config.getGroq().getModels().get("default"));
    }
    
    @Override
    public List<Question> generateQuestions(String topic, String subject, int count, String difficulty) {
        if (!isAvailable()) {
            throw new AIProviderException("Groq", "Groq API is not available or not configured");
        }
        
        String systemPrompt = "You are an educational quiz generator. Generate questions in valid JSON format only.";
        
        String userPrompt = String.format(
            "Generate %d multiple-choice questions about '%s' in the subject of %s at %s difficulty level. " +
            "Return ONLY a valid JSON array with this exact structure (no markdown, no code blocks):\n" +
            "[{\"question\":\"text\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctIndex\":0,\"explanation\":\"text\"}]\n" +
            "Make questions educational and age-appropriate.",
            count, topic, subject, difficulty
        );
        
        String response = callGroqAPI(systemPrompt, userPrompt, config.getGroq().getModels().get("default"));
        return parseQuestionsFromJSON(response);
    }
    
    @Override
    public String generateHint(String query, String subject, int age) {
        if (!isAvailable()) {
            throw new AIProviderException("Groq", "Groq API is not available or not configured");
        }
        
        String systemPrompt = "You are a helpful educational assistant that provides hints without giving away answers.";
        
        String userPrompt = String.format(
            "For a %d-year-old learning about %s, provide a helpful hint (not the full answer) for: %s\n" +
            "The hint should guide their thinking without giving away the answer. Keep it under 50 words.",
            age, subject, query
        );
        
        return callGroqAPI(systemPrompt, userPrompt, config.getGroq().getModels().get("default"));
    }
    
    @Override
    public String analyzeQuerySubject(String query) {
        if (!isAvailable()) {
            throw new AIProviderException("Groq", "Groq API is not available or not configured");
        }
        
        String systemPrompt = "You are a subject classifier. Return only the subject name, nothing else.";
        
        String userPrompt = String.format(
            "Analyze this question and return ONLY the subject category (one word): %s\n" +
            "Choose from: Mathematics, Science, English, History, Geography, Computer Science, Art, Music, General",
            query
        );
        
        String response = callGroqAPI(systemPrompt, userPrompt, config.getGroq().getModels().get("default"));
        return response.trim().split("\\s+")[0]; // Get first word
    }
    
    private String callGroqAPI(String systemPrompt, String userPrompt, String model) {
        try {
            // Groq uses OpenAI-compatible API format
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", config.getGroq().getTemperature(),
                "max_tokens", config.getGroq().getMaxTokens()
            );
            
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        if (clientResponse.statusCode().value() == 429) {
                            return Mono.error(new RateLimitException("Groq", "Rate limit exceeded"));
                        }
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new AIProviderException("Groq", "Client error: " + body)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new AIProviderException("Groq", "Server error: " + body))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getGroq().getTimeoutSeconds()))
                    .block();
            
            return extractTextFromOpenAIResponse(response);
            
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Groq API: {}", e.getMessage(), e);
            throw new AIProviderException("Groq", "Failed to call Groq API: " + e.getMessage(), e);
        }
    }
    
    private String extractTextFromOpenAIResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new AIProviderException("Groq", "Unexpected response format");
        } catch (Exception e) {
            log.error("Error parsing Groq response: {}", e.getMessage());
            throw new AIProviderException("Groq", "Failed to parse response", e);
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
            throw new AIProviderException("Groq", "Failed to parse questions from response", e);
        }
        
        return questions;
    }
}

