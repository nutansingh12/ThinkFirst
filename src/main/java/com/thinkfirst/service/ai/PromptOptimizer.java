package com.thinkfirst.service.ai;

import org.springframework.stereotype.Component;

/**
 * Optimized prompt templates to minimize token usage
 * Target: 30-50% token reduction while maintaining quality
 */
@Component
public class PromptOptimizer {
    
    /**
     * Optimized educational response prompt
     * Before: ~150 tokens | After: ~50 tokens (67% reduction)
     */
    public String buildEducationalPrompt(String query, int age, String subject) {
        return String.format(
            "Age %d. %s. Explain: %s\nConcise, clear, <150 words.",
            age, subject, query
        );
    }
    
    /**
     * Optimized quiz generation prompt
     * Before: ~200 tokens | After: ~80 tokens (60% reduction)
     */
//    public String buildQuizPrompt(String topic, String subject, int count, String difficulty) {
//        return String.format(
//            "%d MCQs on '%s' (%s, %s level).\nJSON only:\n" +
//            "[{\"question\":\"What is 2+2?\",\"options\":[\"3\",\"4\",\"5\",\"6\"],\"correctIndex\":1,\"explanation\":\"2+2=4\"}]\n" +
//            "4 real answer options each (not A,B,C,D).",
//            count, topic, subject, difficulty.toLowerCase()
//        );
//    }

    public String buildQuizPrompt(String query, String subject, int count, String difficulty, Integer age) {
        return String.format(
            "You are an expert early childhood educator who understands cognitive development stages for children aged 5–16.\n" +
                    "\n" +
                    "Your task:\n" +
                    "Given a child’s age and their question and their skill level (beginner, intermediate, advanced, expert), generate a short, " +
                    "age-appropriate sequence of prerequisite questions that assess and build the child’s foundational understanding " +
                    "before directly answering their main question.\n" +
                    "\n" +
                    "Guidelines:\n" +
                    "1. Use simple, playful language that matches the child's age.\n" +
                    "2. Start from what the child likely already knows and gently scaffold up to the main concept.\n" +
                    "3. Use tangible, real-world examples (toys, fruits, fingers, animals, etc.) instead of abstract math or text.\n" +
                    "4. Each question should have a clear purpose — number sense, grouping, pattern recognition, etc.\n" +
                    "5. Avoid giving answers. Focus on prompting thinking.\n" +
                    "6. Include 3–6 questions, depending on the child’s age and complexity of the concept.\n" +
                    "7. Format output as a short, numbered list of questions.\n" +
                    "8. Optionally include a 1-line explanation of what each question checks (for the parent/teacher or app to use internally).\n" +
                    "\n" +
                    "Input variables:\n" +
                    "- Child age: {{age}}\n" +
                    "- Subject: {{subject}}\n" +
                    "- Child's main question: \"{{query}}\"\n" +
                    "- Skill level: {{difficulty}}\n" +
                    "- Number of questions: {{count}}\n" +
                    "\n" +
                    "Output:\n" +
                    "- A progression of prerequisite questions appropriate for a {{age}}-year-old child leading up to understanding \"{{child_question}}\".\n",
            count, subject, query, difficulty, age
        );
    }
    
    /**
     * Optimized hint generation prompt
     * Before: ~100 tokens | After: ~30 tokens (70% reduction)
     */
    public String buildHintPrompt(String query, int age, String subject) {
        return String.format(
            "Age %d, %s. Hint (not answer) for: %s\n<40 words.",
            age, subject, query
        );
    }
    
    /**
     * Optimized subject classification prompt
     * Before: ~80 tokens | After: ~20 tokens (75% reduction)
     */
    public String buildSubjectPrompt(String query) {
        return String.format(
            "Subject (1 word): %s\nOptions: Math, Science, English, History, Geography, CS, Art, Music, General",
            query
        );
    }
    
    /**
     * System prompts (reusable, minimal)
     */
    public static class SystemPrompts {
        public static final String EDUCATOR = "Educational AI tutor. Clear, age-appropriate.";
        public static final String QUIZ_GENERATOR = "Quiz generator. JSON only.";
        public static final String HINT_PROVIDER = "Hint provider. Guide, don't answer.";
        public static final String CLASSIFIER = "Subject classifier.";
    }
    
    /**
     * Abbreviations for common terms (further optimization)
     */
    public String abbreviate(String text) {
        return text
            .replace("multiple-choice", "MC")
            .replace("questions", "Qs")
            .replace("explanation", "exp")
            .replace("difficulty", "diff")
            .replace("Mathematics", "Math")
            .replace("Computer Science", "CS");
    }
}

