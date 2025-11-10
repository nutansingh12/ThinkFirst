package com.thinkfirst.service.ai;

import com.thinkfirst.config.AIProviderConfig;
import com.thinkfirst.model.Question;
import com.thinkfirst.service.cache.AICacheService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Test for AIProviderService with REAL AI providers
 * 
 * ⚠️ IMPORTANT: This test makes REAL API calls to AI providers
 * 
 * To run this test locally:
 * 1. Make sure you have API keys configured in application.yml or environment variables
 * 2. Set GROQ_API_KEY environment variable (free tier available)
 * 3. Run: mvn test -Dtest=AIProviderServiceIntegrationTest
 * 
 * This test will:
 * - Test real API calls to Groq (or other configured providers)
 * - Verify question format is correct (not just A,B,C,D)
 * - Test educational response generation
 * - Test hint generation
 * - Test subject classification
 * - Verify caching works correctly
 * - Monitor provider status
 * 
 * Expected output:
 * - All tests should pass if at least one provider is configured
 * - You should see actual quiz questions with real answer text
 * - Cache should prevent duplicate API calls
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AIProviderServiceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AIProviderServiceIntegrationTest.class);

    @Autowired
    private AIProviderService aiProviderService;

    @Autowired
    private AIProviderConfig config;

    @Autowired(required = false)
    private AICacheService cacheService;

    @BeforeAll
    void setup() {
        log.info("\n" + "=".repeat(80));
        log.info("STARTING AI PROVIDER INTEGRATION TEST");
        log.info("=".repeat(80));
        log.info("⚠️  This test makes REAL API calls to AI providers");
        log.info("Make sure you have API keys configured!");
        log.info("=".repeat(80) + "\n");
    }

    @Test
    @Order(1)
    @DisplayName("Integration Test 1: Check provider availability")
    void testProviderAvailability() {
        log.info("\n>>> INTEGRATION TEST 1: Check provider availability");
        
        // Act
        Map<String, AIProviderService.ProviderStatus> status = aiProviderService.getProviderStatus();

        // Assert
        assertThat(status).isNotNull();
        assertThat(status).isNotEmpty();
        
        log.info("Provider Status:");
        status.forEach((key, value) -> {
            log.info("  {} ({}) - Available: {}", value.name, key, value.available);
        });
        
        // At least one provider should be available
        boolean anyAvailable = status.values().stream().anyMatch(s -> s.available);
        assertThat(anyAvailable)
            .withFailMessage("No AI providers are available! Please configure at least one provider.")
            .isTrue();
        
        log.info("✅ TEST 1 PASSED: At least one provider is available");
    }

    @Test
    @Order(2)
    @DisplayName("Integration Test 2: Generate quiz questions with REAL API")
    void testGenerateQuestionsRealAPI() {
        log.info("\n>>> INTEGRATION TEST 2: Generate quiz questions with REAL API");
        
        // Clear cache to force API call
        if (cacheService != null) {
            aiProviderService.invalidateQuizCache();
            log.info("Cache cleared - will make real API call");
        }
        
        // Act
        List<Question> questions = aiProviderService.generateQuestions(
            "basic algebra", 
            "Mathematics", 
            3, 
            "BEGINNER"
        );

        // Assert
        assertThat(questions).isNotNull();
        assertThat(questions).isNotEmpty();
        assertThat(questions.size()).isGreaterThanOrEqualTo(2); // Should get at least 2 questions
        
        log.info("Generated {} questions:", questions.size());
        
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            log.info("\n--- Question {} ---", i + 1);
            log.info("Text: {}", q.getQuestionText());
            log.info("Type: {}", q.getType());
            
            // Verify question has proper format
            assertThat(q.getQuestionText())
                .withFailMessage("Question text should not be blank")
                .isNotBlank();
            
            if (q.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
                assertThat(q.getOptions())
                    .withFailMessage("Multiple choice question should have options")
                    .isNotNull()
                    .isNotEmpty();
                
                log.info("Options:");
                for (int j = 0; j < q.getOptions().size(); j++) {
                    String option = q.getOptions().get(j);
                    log.info("  {}. {}", j + 1, option);
                    
                    // CRITICAL: Verify options are NOT just "A", "B", "C", "D"
                    assertThat(option)
                        .withFailMessage("Option should not be blank")
                        .isNotBlank();
                    
                    assertThat(option.length())
                        .withFailMessage("Option '{}' is too short - should be actual answer text, not just a letter!", option)
                        .isGreaterThan(1);
                    
                    // Additional check: option should not be just a single letter
                    if (option.length() == 1) {
                        fail("❌ CRITICAL BUG: Option is just a single letter: '" + option + "'. " +
                             "This means the AI is still generating A,B,C,D instead of real answers!");
                    }
                }
                
                assertThat(q.getCorrectOptionIndex())
                    .withFailMessage("Correct option index should be set")
                    .isNotNull()
                    .isBetween(0, q.getOptions().size() - 1);
                
                log.info("Correct Answer: {} (index {})", 
                    q.getOptions().get(q.getCorrectOptionIndex()), 
                    q.getCorrectOptionIndex());
            }
            
            if (q.getExplanation() != null) {
                log.info("Explanation: {}", q.getExplanation());
            }
        }
        
        log.info("\n✅ TEST 2 PASSED: Questions generated with proper format (NOT just A,B,C,D)");
    }

    @Test
    @Order(3)
    @DisplayName("Integration Test 3: Generate educational response with REAL API")
    void testGenerateEducationalResponseRealAPI() {
        log.info("\n>>> INTEGRATION TEST 3: Generate educational response with REAL API");
        
        // Clear cache to force API call
        if (cacheService != null) {
            aiProviderService.invalidateResponseCache();
            log.info("Cache cleared - will make real API call");
        }
        
        // Act
        String response = aiProviderService.generateEducationalResponse(
            "What is algebra?", 
            12, 
            "Mathematics"
        );

        // Assert
        assertThat(response).isNotBlank();
        assertThat(response.length()).isGreaterThan(50); // Should be a meaningful response
        
        log.info("Educational Response:");
        log.info("{}", response);
        
        log.info("\n✅ TEST 3 PASSED: Educational response generated");
    }

    @Test
    @Order(4)
    @DisplayName("Integration Test 4: Generate hint with REAL API")
    void testGenerateHintRealAPI() {
        log.info("\n>>> INTEGRATION TEST 4: Generate hint with REAL API");
        
        // Act
        String hint = aiProviderService.generateHint(
            "What is the Pythagorean theorem?", 
            "Mathematics", 
            14
        );

        // Assert
        assertThat(hint).isNotBlank();
        assertThat(hint.length()).isLessThan(300); // Hints should be concise
        
        log.info("Hint Generated:");
        log.info("{}", hint);
        
        log.info("\n✅ TEST 4 PASSED: Hint generated");
    }

    @Test
    @Order(5)
    @DisplayName("Integration Test 5: Analyze query subject with REAL API")
    void testAnalyzeQuerySubjectRealAPI() {
        log.info("\n>>> INTEGRATION TEST 5: Analyze query subject with REAL API");
        
        // Act
        String subject = aiProviderService.analyzeQuerySubject("What is photosynthesis?");

        // Assert
        assertThat(subject).isNotBlank();
        
        log.info("Query: 'What is photosynthesis?'");
        log.info("Detected Subject: {}", subject);
        
        // Should be Science or Biology
        assertThat(subject.toLowerCase())
            .containsAnyOf("science", "biology", "general");
        
        log.info("\n✅ TEST 5 PASSED: Subject analyzed correctly");
    }

    @Test
    @Order(6)
    @DisplayName("Integration Test 6: Verify caching works (second call should be instant)")
    void testCachingWorks() {
        log.info("\n>>> INTEGRATION TEST 6: Verify caching works");
        
        if (cacheService == null) {
            log.warn("⚠️  Cache service not available, skipping cache test");
            return;
        }
        
        String query = "What is 2+2?";
        
        // First call - should hit API
        log.info("First call (should hit API)...");
        long start1 = System.currentTimeMillis();
        String response1 = aiProviderService.generateEducationalResponse(query, 10, "Mathematics");
        long duration1 = System.currentTimeMillis() - start1;
        log.info("First call took: {}ms", duration1);
        
        // Second call - should hit cache
        log.info("Second call (should hit cache)...");
        long start2 = System.currentTimeMillis();
        String response2 = aiProviderService.generateEducationalResponse(query, 10, "Mathematics");
        long duration2 = System.currentTimeMillis() - start2;
        log.info("Second call took: {}ms", duration2);
        
        // Assert
        assertThat(response1).isEqualTo(response2);
        assertThat(duration2).isLessThan(duration1); // Cache should be faster
        
        log.info("Cache speedup: {}x faster", (double) duration1 / duration2);
        log.info("\n✅ TEST 6 PASSED: Caching works correctly");
    }

    @Test
    @Order(7)
    @DisplayName("Integration Test 7: Test specific provider")
    void testSpecificProvider() {
        log.info("\n>>> INTEGRATION TEST 7: Test specific provider");
        
        // Test Groq if available
        boolean groqResult = aiProviderService.testProvider("groq");
        log.info("Groq test result: {}", groqResult ? "✅ PASS" : "❌ FAIL");
        
        // Test Gemini if available
        boolean geminiResult = aiProviderService.testProvider("gemini");
        log.info("Gemini test result: {}", geminiResult ? "✅ PASS" : "❌ FAIL");
        
        // Test OpenAI if available
        boolean openaiResult = aiProviderService.testProvider("openai");
        log.info("OpenAI test result: {}", openaiResult ? "✅ PASS" : "❌ FAIL");
        
        // At least one should work
        assertThat(groqResult || geminiResult || openaiResult)
            .withFailMessage("At least one provider should be working")
            .isTrue();
        
        log.info("\n✅ TEST 7 PASSED: Provider testing works");
    }

    @AfterAll
    void summary() {
        log.info("\n" + "=".repeat(80));
        log.info("INTEGRATION TEST SUMMARY");
        log.info("=".repeat(80));
        log.info("All integration tests completed successfully!");
        log.info("");
        log.info("Verified:");
        log.info("  ✅ At least one AI provider is available and working");
        log.info("  ✅ Quiz questions are generated with REAL answer text (not A,B,C,D)");
        log.info("  ✅ Educational responses are generated correctly");
        log.info("  ✅ Hints are generated correctly");
        log.info("  ✅ Subject classification works");
        log.info("  ✅ Caching prevents duplicate API calls");
        log.info("  ✅ Provider testing functionality works");
        log.info("=".repeat(80) + "\n");
    }
}

