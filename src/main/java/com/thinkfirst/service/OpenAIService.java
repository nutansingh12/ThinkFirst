package com.thinkfirst.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.thinkfirst.model.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(com.theokanning.openai.service.OpenAiService.class)
public class OpenAIService {

    private final com.theokanning.openai.service.OpenAiService openAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${openai.api.model}")
    private String model;
    
    @Value("${openai.api.max-tokens}")
    private Integer maxTokens;
    
    @Value("${openai.api.temperature}")
    private Double temperature;
    
    /**
     * Get educational response from OpenAI
     */
    public String getEducationalResponse(String query, Integer childAge) {
        String systemPrompt = buildEducationalSystemPrompt(childAge);
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(Arrays.asList(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                        new ChatMessage(ChatMessageRole.USER.value(), query)
                ))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
        
        try {
            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return "I'm having trouble answering that right now. Please try again!";
        }
    }
    
    /**
     * Generate quiz questions using OpenAI
     */
    public List<Question> generateQuestions(String topic, Integer count, String difficulty, Integer childAge) {
        String prompt = buildQuizGenerationPrompt(topic, count, difficulty, childAge);
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(
                        new ChatMessage(ChatMessageRole.USER.value(), prompt)
                ))
                .maxTokens(1000)
                .temperature(0.7)
                .build();
        
        try {
            String response = openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
            
            return parseQuestionsFromJSON(response);
        } catch (Exception e) {
            log.error("Error generating questions", e);
            return generateFallbackQuestions(topic, count);
        }
    }
    
    /**
     * Analyze query to determine subject
     */
    public String analyzeQuerySubject(String query) {
        String prompt = String.format(
                "Analyze this question and return ONLY the subject category (Math, Science, English, History, or General): '%s'",
                query
        );
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(new ChatMessage(ChatMessageRole.USER.value(), prompt)))
                .maxTokens(10)
                .temperature(0.3)
                .build();
        
        try {
            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent()
                    .trim();
        } catch (Exception e) {
            log.error("Error analyzing query subject", e);
            return "General";
        }
    }
    
    /**
     * Generate a hint instead of full answer
     */
    public String generateHint(String query, Integer childAge) {
        String prompt = String.format(
                "For this question: '%s'\n\nProvide a helpful HINT (not the full answer) that guides a %d-year-old to think about the problem. Ask guiding questions.",
                query, childAge
        );
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(new ChatMessage(ChatMessageRole.USER.value(), prompt)))
                .maxTokens(150)
                .temperature(0.7)
                .build();
        
        try {
            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        } catch (Exception e) {
            log.error("Error generating hint", e);
            return "Think about what you already know about this topic. What are the key concepts involved?";
        }
    }
    
    private String buildEducationalSystemPrompt(Integer childAge) {
        return String.format("""
                You are an educational AI tutor for children around %d years old.
                Your role is to:
                1. Explain concepts clearly using age-appropriate language
                2. Encourage critical thinking and curiosity
                3. Use examples and analogies that kids can relate to
                4. Be patient, supportive, and encouraging
                5. Break down complex topics into simple steps
                6. Never give direct answers to homework - guide them to discover
                7. Keep responses concise (under 200 words)
                8. Use a friendly, enthusiastic tone
                
                Always prioritize learning over just providing answers.
                """, childAge);
    }
    
    private String buildQuizGenerationPrompt(String topic, Integer count, String difficulty, Integer childAge) {
        return String.format("""
                Generate %d multiple-choice questions about: %s
                Difficulty: %s
                Age group: %d years old
                
                Return ONLY a JSON array with this exact format:
                [
                  {
                    "question": "Question text here?",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctIndex": 0,
                    "explanation": "Why this is correct"
                  }
                ]
                
                Make questions engaging and age-appropriate.
                """, count, topic, difficulty, childAge);
    }
    
    private List<Question> parseQuestionsFromJSON(String jsonResponse) {
        List<Question> questions = new ArrayList<>();
        
        try {
            // Extract JSON array from response (in case there's extra text)
            String cleanJson = jsonResponse;
            if (jsonResponse.contains("[")) {
                cleanJson = jsonResponse.substring(jsonResponse.indexOf("["), jsonResponse.lastIndexOf("]") + 1);
            }
            
            JsonNode jsonArray = objectMapper.readTree(cleanJson);
            
            for (JsonNode node : jsonArray) {
                Question question = Question.builder()
                        .questionText(node.get("question").asText())
                        .type(Question.QuestionType.MULTIPLE_CHOICE)
                        .options(parseOptions(node.get("options")))
                        .correctOptionIndex(node.get("correctIndex").asInt())
                        .correctAnswer(node.get("options").get(node.get("correctIndex").asInt()).asText())
                        .explanation(node.has("explanation") ? node.get("explanation").asText() : "")
                        .build();
                
                questions.add(question);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing questions JSON", e);
        }
        
        return questions;
    }
    
    private List<String> parseOptions(JsonNode optionsNode) {
        List<String> options = new ArrayList<>();
        optionsNode.forEach(option -> options.add(option.asText()));
        return options;
    }
    
    private List<Question> generateFallbackQuestions(String topic, Integer count) {
        // Fallback questions if OpenAI fails
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            questions.add(Question.builder()
                    .questionText("What do you know about " + topic + "?")
                    .type(Question.QuestionType.SHORT_ANSWER)
                    .correctAnswer("Any reasonable answer")
                    .build());
        }
        return questions;
    }
}

