package com.thinkfirst.service.ai;

import com.thinkfirst.dto.QuizGenerationResult;
import com.thinkfirst.model.Question;

import java.util.List;

/**
 * Interface for AI providers (OpenAI, Gemini, Groq, etc.)
 */
public interface AIProvider {

    /**
     * Get the provider name
     */
    String getProviderName();

    /**
     * Check if the provider is available and configured
     */
    boolean isAvailable();

    /**
     * Generate an educational response for a query
     */
    String generateEducationalResponse(String query, int age, String subject);

    /**
     * Generate detailed learning lessons (requires higher token limit)
     */
    String generateLearningLessons(String prompt, int age, String subject);

    /**
     * Generate quiz questions based on a topic
     */
    List<Question> generateQuestions(String topic, String subject, int count, String difficulty, Integer age);

    /**
     * Generate quiz questions with subject detection in a single call
     * This is more efficient as it detects the subject and generates questions in one API call
     *
     * Default implementation: analyze subject separately then generate questions
     * Providers should override this for better efficiency
     */
    default QuizGenerationResult generateQuestionsWithSubject(String query, int count, String difficulty, Integer age) {
        // Fallback: analyze subject first, then generate questions
        String subject = analyzeQuerySubject(query);
        List<Question> questions = generateQuestions(query, subject, count, difficulty, age);
        return QuizGenerationResult.builder()
                .detectedSubject(subject)
                .questions(questions)
                .build();
    }

    /**
     * Generate a hint for a question
     */
    String generateHint(String query, String subject, int age);

    /**
     * Analyze a query to determine the subject
     */
    String analyzeQuerySubject(String query);
}

