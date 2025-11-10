package com.thinkfirst.service.ai;

import com.thinkfirst.config.AIProviderConfig;
import com.thinkfirst.exception.AIProviderException;
import com.thinkfirst.model.Question;
import com.thinkfirst.service.cache.AICacheService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Debug Test for AIProviderService
 * Run this locally to thoroughly test all AI provider functionality
 *
 * This test covers:
 * 1. Provider availability and fallback logic
 * 2. Question generation with proper format
 * 3. Educational response generation
 * 4. Hint generation
 * 5. Subject analysis
 * 6. Cache integration
 * 7. Error handling and recovery
 * 8. Provider status monitoring
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIProviderServiceDebugTest {

    private static final Logger log = LoggerFactory.getLogger(AIProviderServiceDebugTest.class);

    @Mock
    private GeminiService geminiService;

    @Mock
    private GroqService groqService;

    @Mock
    private OpenAIProviderService openAIService;

    @Mock
    private AIProviderConfig config;

    @Mock
    private AICacheService cacheService;

    private AIProviderService aiProviderService;

    // Test data
    private List<Question> mockQuestions;
    private String mockEducationalResponse;
    private String mockHint;
    private String mockSubject;

    @BeforeEach
    void setUp() {
        log.info("=== Setting up AIProviderService Debug Test ===");
        
        // Setup mock questions with proper format
        mockQuestions = createMockQuestions();
        mockEducationalResponse = "Algebra is a branch of mathematics that uses symbols and letters to represent numbers and quantities in formulas and equations.";
        mockHint = "Think about what branch of mathematics uses variables like x and y.";
        mockSubject = "Mathematics";

        // Setup provider priority
        when(config.getProviderPriority()).thenReturn(Arrays.asList("gemini", "groq", "openai"));

        // Setup provider names
        when(geminiService.getProviderName()).thenReturn("Gemini");
        when(groqService.getProviderName()).thenReturn("Groq");
        when(openAIService.getProviderName()).thenReturn("OpenAI");

        // Initialize service
        aiProviderService = new AIProviderService(
            geminiService,
            groqService,
            openAIService,
            config,
            cacheService
        );

        log.info("Setup complete - AIProviderService initialized");
    }

    // ========== PROVIDER AVAILABILITY TESTS ==========

    @Test
    @Order(1)
    @DisplayName("Test 1: All providers available - should use first priority (Gemini)")
    void testAllProvidersAvailable() {
        log.info("\n>>> TEST 1: All providers available");
        
        // Arrange
        when(geminiService.isAvailable()).thenReturn(true);
        when(groqService.isAvailable()).thenReturn(true);
        when(openAIService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedResponse(anyString(), anyInt(), anyString())).thenReturn(Optional.empty());
        when(geminiService.generateEducationalResponse(anyString(), anyInt(), anyString()))
            .thenReturn(mockEducationalResponse);

        // Act
        String response = aiProviderService.generateEducationalResponse("What is algebra?", 12, "Mathematics");

        // Assert
        assertThat(response).isEqualTo(mockEducationalResponse);
        verify(geminiService, times(1)).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(groqService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(openAIService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        
        log.info("✅ TEST 1 PASSED: Gemini was used as first priority");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Gemini unavailable - should fallback to Groq")
    void testFallbackToGroq() {
        log.info("\n>>> TEST 2: Gemini unavailable, fallback to Groq");
        
        // Arrange
        when(geminiService.isAvailable()).thenReturn(false);
        when(groqService.isAvailable()).thenReturn(true);
        when(openAIService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedResponse(anyString(), anyInt(), anyString())).thenReturn(Optional.empty());
        when(groqService.generateEducationalResponse(anyString(), anyInt(), anyString()))
            .thenReturn(mockEducationalResponse);

        // Act
        String response = aiProviderService.generateEducationalResponse("What is algebra?", 12, "Mathematics");

        // Assert
        assertThat(response).isEqualTo(mockEducationalResponse);
        verify(geminiService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(groqService, times(1)).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(openAIService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        
        log.info("✅ TEST 2 PASSED: Groq was used as fallback");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Gemini and Groq unavailable - should fallback to OpenAI")
    void testFallbackToOpenAI() {
        log.info("\n>>> TEST 3: Gemini and Groq unavailable, fallback to OpenAI");
        
        // Arrange
        when(geminiService.isAvailable()).thenReturn(false);
        when(groqService.isAvailable()).thenReturn(false);
        when(openAIService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedResponse(anyString(), anyInt(), anyString())).thenReturn(Optional.empty());
        when(openAIService.generateEducationalResponse(anyString(), anyInt(), anyString()))
            .thenReturn(mockEducationalResponse);

        // Act
        String response = aiProviderService.generateEducationalResponse("What is algebra?", 12, "Mathematics");

        // Assert
        assertThat(response).isEqualTo(mockEducationalResponse);
        verify(geminiService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(groqService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(openAIService, times(1)).generateEducationalResponse(anyString(), anyInt(), anyString());
        
        log.info("✅ TEST 3 PASSED: OpenAI was used as final fallback");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: All providers unavailable - should throw exception")
    void testAllProvidersUnavailable() {
        log.info("\n>>> TEST 4: All providers unavailable");
        
        // Arrange
        when(geminiService.isAvailable()).thenReturn(false);
        when(groqService.isAvailable()).thenReturn(false);
        when(openAIService.isAvailable()).thenReturn(false);
        when(cacheService.getCachedResponse(anyString(), anyInt(), anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> 
            aiProviderService.generateEducationalResponse("What is algebra?", 12, "Mathematics")
        )
        .isInstanceOf(AIProviderException.class)
        .hasMessageContaining("All AI providers failed");
        
        log.info("✅ TEST 4 PASSED: Exception thrown when all providers unavailable");
    }

    // ========== QUESTION GENERATION TESTS ==========

    @Test
    @Order(5)
    @DisplayName("Test 5: Generate questions with proper format")
    void testGenerateQuestionsWithProperFormat() {
        log.info("\n>>> TEST 5: Generate questions with proper format");
        
        // Arrange
        when(groqService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedQuiz(anyString(), anyString(), anyInt(), anyString(), anyInt()))
            .thenReturn(Optional.empty());
        when(groqService.generateQuestions(anyString(), anyString(), anyInt(), anyString(), anyInt()))
            .thenReturn(mockQuestions);

        // Act
        List<Question> questions = aiProviderService.generateQuestions(
            "what is a moonlight?", "Mathematics", 3, "BEGINNER",
                8);

        // Assert
        assertThat(questions).isNotNull();
        assertThat(questions).hasSize(3);
        
        // Verify each question has proper format
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            log.info("Question {}: {}", i + 1, q.getQuestionText());
            
            assertThat(q.getQuestionText()).isNotBlank();
            assertThat(q.getOptions()).isNotNull();
            assertThat(q.getOptions()).hasSize(4);
            assertThat(q.getCorrectOptionIndex()).isNotNull();
            assertThat(q.getCorrectOptionIndex()).isBetween(0, 3);
            assertThat(q.getExplanation()).isNotBlank();
            
            // Verify options are NOT just "A", "B", "C", "D"
            for (String option : q.getOptions()) {
                log.info("  Option: {}", option);
                assertThat(option).isNotBlank();
                assertThat(option.length()).isGreaterThan(1); // Should be actual text, not just a letter
            }
        }
        
        verify(cacheService, times(1)).cacheQuiz(anyString(), anyString(), anyInt(), anyString(), anyList(), anyInt());
        log.info("✅ TEST 5 PASSED: Questions generated with proper format");
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Question generation with Gemini failure, Groq success")
    void testQuestionGenerationWithFallback() {
        log.info("\n>>> TEST 6: Question generation with provider fallback");
        
        // Arrange
        when(geminiService.isAvailable()).thenReturn(true);
        when(groqService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedQuiz(anyString(), anyString(), anyInt(), anyString(), anyInt()))
            .thenReturn(Optional.empty());
        when(geminiService.generateQuestions(anyString(), anyString(), anyInt(), anyString(), anyInt()))
            .thenThrow(new AIProviderException("Gemini", "Rate limit exceeded"));
        when(groqService.generateQuestions(anyString(), anyString(), anyInt(), anyString(), anyInt()))
            .thenReturn(mockQuestions);

        // Act
        List<Question> questions = aiProviderService.generateQuestions(
            "algebra basics", "Mathematics", 3, "BEGINNER",
                8);

        // Assert
        assertThat(questions).isNotNull();
        assertThat(questions).hasSize(3);
        verify(geminiService, times(1)).generateQuestions(anyString(), anyString(), anyInt(), anyString(), anyInt());
        verify(groqService, times(1)).generateQuestions(anyString(), anyString(), anyInt(), anyString(), anyInt());
        
        log.info("✅ TEST 6 PASSED: Fallback from Gemini to Groq successful");
    }

    // ========== CACHE TESTS ==========

    @Test
    @Order(7)
    @DisplayName("Test 7: Cache hit - should not call AI provider")
    void testCacheHit() {
        log.info("\n>>> TEST 7: Cache hit scenario");
        
        // Arrange
        when(cacheService.getCachedResponse(anyString(), anyInt(), anyString()))
            .thenReturn(Optional.of(mockEducationalResponse));

        // Act
        String response = aiProviderService.generateEducationalResponse("What is algebra?", 12, "Mathematics");

        // Assert
        assertThat(response).isEqualTo(mockEducationalResponse);
        verify(geminiService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(groqService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(openAIService, never()).generateEducationalResponse(anyString(), anyInt(), anyString());
        
        log.info("✅ TEST 7 PASSED: Cache hit prevented AI API call");
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Quiz cache hit - should not call AI provider")
    void testQuizCacheHit() {
        log.info("\n>>> TEST 8: Quiz cache hit scenario");

        // Arrange
        when(cacheService.getCachedQuiz(anyString(), anyString(), anyInt(), anyString(), anyInt()))
            .thenReturn(Optional.of(mockQuestions));

        // Act
        List<Question> questions = aiProviderService.generateQuestions(
            "algebra basics", "Mathematics", 3, "BEGINNER",
                8);

        // Assert
        assertThat(questions).isNotNull();
        assertThat(questions).hasSize(3);
        verify(geminiService, never()).generateQuestions(anyString(), anyString(), anyInt(), anyString(), anyInt());
        verify(groqService, never()).generateQuestions(anyString(), anyString(), anyInt(), anyString(), anyInt());
        verify(openAIService, never()).generateQuestions(anyString(), anyString(), anyInt(), anyString(), anyInt());

        log.info("✅ TEST 8 PASSED: Quiz cache hit prevented AI API call");
    }

    // ========== HINT GENERATION TESTS ==========

    @Test
    @Order(9)
    @DisplayName("Test 9: Generate hint successfully")
    void testGenerateHint() {
        log.info("\n>>> TEST 9: Generate hint");

        // Arrange
        when(groqService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedHint(anyString(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(groqService.generateHint(anyString(), anyString(), anyInt())).thenReturn(mockHint);

        // Act
        String hint = aiProviderService.generateHint("What is algebra?", "Mathematics", 12);

        // Assert
        assertThat(hint).isEqualTo(mockHint);
        assertThat(hint).isNotBlank();
        assertThat(hint.length()).isLessThan(200); // Hints should be concise
        verify(cacheService, times(1)).cacheHint(anyString(), anyString(), anyInt(), anyString());

        log.info("✅ TEST 9 PASSED: Hint generated: {}", hint);
    }

    // ========== SUBJECT ANALYSIS TESTS ==========

    @Test
    @Order(10)
    @DisplayName("Test 10: Analyze query subject successfully")
    void testAnalyzeQuerySubject() {
        log.info("\n>>> TEST 10: Analyze query subject");

        // Arrange
        when(groqService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedSubject(anyString())).thenReturn(Optional.empty());
        when(groqService.analyzeQuerySubject(anyString())).thenReturn(mockSubject);

        // Act
        String subject = aiProviderService.analyzeQuerySubject("What is algebra?");

        // Assert
        assertThat(subject).isEqualTo("Mathematics");
        verify(cacheService, times(1)).cacheSubject(anyString(), anyString());

        log.info("✅ TEST 10 PASSED: Subject identified: {}", subject);
    }

    @Test
    @Order(11)
    @DisplayName("Test 11: Subject analysis with cache hit")
    void testSubjectAnalysisWithCache() {
        log.info("\n>>> TEST 11: Subject analysis with cache hit");

        // Arrange
        when(cacheService.getCachedSubject(anyString())).thenReturn(Optional.of("Mathematics"));

        // Act
        String subject = aiProviderService.analyzeQuerySubject("What is algebra?");

        // Assert
        assertThat(subject).isEqualTo("Mathematics");
        verify(groqService, never()).analyzeQuerySubject(anyString());

        log.info("✅ TEST 11 PASSED: Subject from cache: {}", subject);
    }

    // ========== PROVIDER STATUS TESTS ==========

    @Test
    @Order(12)
    @DisplayName("Test 12: Get provider status")
    void testGetProviderStatus() {
        log.info("\n>>> TEST 12: Get provider status");

        // Arrange
        when(geminiService.isAvailable()).thenReturn(false);
        when(groqService.isAvailable()).thenReturn(true);
        when(openAIService.isAvailable()).thenReturn(true);

        // Act
        Map<String, AIProviderService.ProviderStatus> status = aiProviderService.getProviderStatus();

        // Assert
        assertThat(status).isNotNull();
        assertThat(status).hasSize(3);
        assertThat(status).containsKeys("gemini", "groq", "openai");

        assertThat(status.get("gemini").available).isFalse();
        assertThat(status.get("groq").available).isTrue();
        assertThat(status.get("openai").available).isTrue();

        log.info("Provider Status:");
        status.forEach((key, value) ->
            log.info("  {} - Available: {}", value.name, value.available)
        );

        log.info("✅ TEST 12 PASSED: Provider status retrieved");
    }

    @Test
    @Order(13)
    @DisplayName("Test 13: Test specific provider")
    void testSpecificProvider() {
        log.info("\n>>> TEST 13: Test specific provider");

        // Arrange
        when(groqService.isAvailable()).thenReturn(true);
        when(groqService.analyzeQuerySubject(anyString())).thenReturn("Mathematics");

        // Act
        boolean result = aiProviderService.testProvider("groq");

        // Assert
        assertThat(result).isTrue();
        verify(groqService, times(1)).analyzeQuerySubject("What is 2+2?");

        log.info("✅ TEST 13 PASSED: Provider test successful");
    }

    @Test
    @Order(14)
    @DisplayName("Test 14: Test non-existent provider")
    void testNonExistentProvider() {
        log.info("\n>>> TEST 14: Test non-existent provider");

        // Act
        boolean result = aiProviderService.testProvider("invalid-provider");

        // Assert
        assertThat(result).isFalse();

        log.info("✅ TEST 14 PASSED: Non-existent provider handled correctly");
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @Order(15)
    @DisplayName("Test 15: Handle AIProviderException and fallback")
    void testHandleAIProviderException() {
        log.info("\n>>> TEST 15: Handle AIProviderException");

        // Arrange
        when(geminiService.isAvailable()).thenReturn(true);
        when(groqService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedResponse(anyString(), anyInt(), anyString())).thenReturn(Optional.empty());
        when(geminiService.generateEducationalResponse(anyString(), anyInt(), anyString()))
            .thenThrow(new AIProviderException("Gemini", "API Error"));
        when(groqService.generateEducationalResponse(anyString(), anyInt(), anyString()))
            .thenReturn(mockEducationalResponse);

        // Act
        String response = aiProviderService.generateEducationalResponse("What is algebra?", 12, "Mathematics");

        // Assert
        assertThat(response).isEqualTo(mockEducationalResponse);
        verify(geminiService, times(1)).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(groqService, times(1)).generateEducationalResponse(anyString(), anyInt(), anyString());

        log.info("✅ TEST 15 PASSED: AIProviderException handled with fallback");
    }

    @Test
    @Order(16)
    @DisplayName("Test 16: Handle generic Exception and fallback")
    void testHandleGenericException() {
        log.info("\n>>> TEST 16: Handle generic Exception");

        // Arrange
        when(geminiService.isAvailable()).thenReturn(true);
        when(groqService.isAvailable()).thenReturn(true);
        when(cacheService.getCachedResponse(anyString(), anyInt(), anyString())).thenReturn(Optional.empty());
        when(geminiService.generateEducationalResponse(anyString(), anyInt(), anyString()))
            .thenThrow(new RuntimeException("Unexpected error"));
        when(groqService.generateEducationalResponse(anyString(), anyInt(), anyString()))
            .thenReturn(mockEducationalResponse);

        // Act
        String response = aiProviderService.generateEducationalResponse("What is algebra?", 12, "Mathematics");

        // Assert
        assertThat(response).isEqualTo(mockEducationalResponse);
        verify(geminiService, times(1)).generateEducationalResponse(anyString(), anyInt(), anyString());
        verify(groqService, times(1)).generateEducationalResponse(anyString(), anyInt(), anyString());

        log.info("✅ TEST 16 PASSED: Generic exception handled with fallback");
    }

    // ========== HELPER METHODS ==========

    private List<Question> createMockQuestions() {
        List<Question> questions = new ArrayList<>();

        // Question 1
        Question q1 = Question.builder()
            .questionText("What is the value of x in the equation 2 + x = 5?")
            .options(Arrays.asList("The value is 1", "The value is 2", "The value is 3", "The value is 4"))
            .correctOptionIndex(2)
            .explanation("2 + 3 = 5, so x = 3")
            .type(Question.QuestionType.MULTIPLE_CHOICE)
            .build();
        questions.add(q1);

        // Question 2
        Question q2 = Question.builder()
            .questionText("Which of the following numbers is greater: 7 or 9?")
            .options(Arrays.asList("Seven is greater", "Nine is greater", "They are equal", "Cannot determine"))
            .correctOptionIndex(1)
            .explanation("9 is greater than 7")
            .type(Question.QuestionType.MULTIPLE_CHOICE)
            .build();
        questions.add(q2);

        // Question 3
        Question q3 = Question.builder()
            .questionText("What is 5 × 3?")
            .options(Arrays.asList("Eight", "Twelve", "Fifteen", "Eighteen"))
            .correctOptionIndex(2)
            .explanation("5 multiplied by 3 equals 15")
            .type(Question.QuestionType.MULTIPLE_CHOICE)
            .build();
        questions.add(q3);

        return questions;
    }

    @AfterEach
    void tearDown() {
        log.info("=== Test completed ===\n");
    }

    @AfterAll
    static void summary() {
        log.info("\n" + "=".repeat(80));
        log.info("ALL TESTS COMPLETED");
        log.info("=".repeat(80));
        log.info("Summary:");
        log.info("  ✅ Provider availability and fallback logic");
        log.info("  ✅ Question generation with proper format");
        log.info("  ✅ Educational response generation");
        log.info("  ✅ Hint generation");
        log.info("  ✅ Subject analysis");
        log.info("  ✅ Cache integration");
        log.info("  ✅ Error handling and recovery");
        log.info("  ✅ Provider status monitoring");
        log.info("=".repeat(80) + "\n");
    }
}

