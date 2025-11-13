package com.thinkfirst.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.dto.LearningPathResponse;
import com.thinkfirst.model.*;
import com.thinkfirst.repository.ChildRepository;
import com.thinkfirst.repository.LearningPathRepository;
import com.thinkfirst.repository.LessonRepository;
import com.thinkfirst.service.ai.AIProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LearningPathService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathService.class);

    private final LearningPathRepository learningPathRepository;
    private final LessonRepository lessonRepository;
    private final ChildRepository childRepository;
    private final AIProviderService aiProviderService;
    private final ObjectMapper objectMapper;

    public LearningPathService(
            LearningPathRepository learningPathRepository,
            LessonRepository lessonRepository,
            ChildRepository childRepository,
            AIProviderService aiProviderService,
            ObjectMapper objectMapper
    ) {
        this.learningPathRepository = learningPathRepository;
        this.lessonRepository = lessonRepository;
        this.childRepository = childRepository;
        this.aiProviderService = aiProviderService;
        this.objectMapper = objectMapper;
    }

    /**
     * Find active learning path by original query and child ID
     * Returns the most recent one if multiple exist
     */
    public Optional<LearningPath> findActiveLearningPath(String originalQuery, Long childId) {
        List<LearningPath> paths = learningPathRepository.findByOriginalQueryAndChildIdAndActiveTrueOrderByCreatedAtDesc(originalQuery, childId);

        if (paths.isEmpty()) {
            return Optional.empty();
        }

        // Return the most recent learning path
        LearningPath mostRecent = paths.get(0);

        // If there are multiple active paths (shouldn't happen, but handle it), deactivate the old ones
        if (paths.size() > 1) {
            log.warn("Found {} active learning paths for query '{}' and child {}. Deactivating old ones.",
                    paths.size(), originalQuery, childId);

            for (int i = 1; i < paths.size(); i++) {
                LearningPath oldPath = paths.get(i);
                oldPath.setActive(false);
                learningPathRepository.save(oldPath);
            }
        }

        return Optional.of(mostRecent);
    }

    /**
     * Generate a learning path for a student who failed a quiz
     */
    @Transactional
    public LearningPathResponse generateLearningPath(
            Long childId,
            Quiz quiz,
            String originalQuery,
            int score,
            int totalQuestions,
            int correctAnswers
    ) {
        log.info("Generating learning path for child {} on topic: {}", childId, originalQuery);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));

        // Check if learning path already exists for this quiz
        LearningPath existingPath = learningPathRepository.findByQuizIdAndChildId(quiz.getId(), childId)
                .orElse(null);

        if (existingPath != null && existingPath.getActive()) {
            log.info("Returning existing learning path: {}", existingPath.getId());
            return buildLearningPathResponse(existingPath, score, totalQuestions, correctAnswers);
        }

        // Generate lessons using AI
        List<Map<String, Object>> generatedLessons = generateLessonsWithAI(
                originalQuery,
                quiz.getSubject().getName(),
                child.getAge()
        );

        // Create learning path
        LearningPath learningPath = LearningPath.builder()
                .child(child)
                .quiz(quiz)
                .topic(quiz.getSubject().getName())
                .originalQuery(originalQuery)
                .motivationalMessage(generateMotivationalMessage(score))
                .totalLessons(generatedLessons.size())
                .completedLessons(0)
                .active(true)
                .build();

        learningPath = learningPathRepository.save(learningPath);

        // Create lessons
        List<Lesson> lessons = new ArrayList<>();
        for (int i = 0; i < generatedLessons.size(); i++) {
            Map<String, Object> lessonData = generatedLessons.get(i);
            
            Lesson lesson = Lesson.builder()
                    .learningPath(learningPath)
                    .title((String) lessonData.get("title"))
                    .description((String) lessonData.get("description"))
                    .content((String) lessonData.get("content"))
                    .displayOrder(i)
                    .completed(false)
                    .resources(parseResources(lessonData.get("resources")))
                    .build();

            lessons.add(lessonRepository.save(lesson));
        }

        learningPath.setLessons(lessons);
        learningPath = learningPathRepository.save(learningPath);

        log.info("Created learning path with {} lessons", lessons.size());

        return buildLearningPathResponse(learningPath, score, totalQuestions, correctAnswers);
    }

    /**
     * Mark a lesson as completed
     */
    @Transactional
    public LearningPathResponse completeLesson(Long childId, Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndLearningPathChildId(lessonId, childId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (!lesson.getCompleted()) {
            lesson.markCompleted();
            lessonRepository.save(lesson);

            LearningPath learningPath = lesson.getLearningPath();
            learningPath.setCompletedLessons(learningPath.getCompletedLessons() + 1);

            if (learningPath.isCompleted()) {
                learningPath.setCompletedAt(java.time.LocalDateTime.now());
            }

            learningPathRepository.save(learningPath);

            log.info("Lesson {} completed. Progress: {}/{}", 
                    lessonId, learningPath.getCompletedLessons(), learningPath.getTotalLessons());
        }

        return buildLearningPathResponse(lesson.getLearningPath(), 0, 0, 0);
    }

    /**
     * Get learning path by ID
     */
    public LearningPathResponse getLearningPath(Long childId, Long learningPathId) {
        LearningPath learningPath = learningPathRepository.findByIdAndChildId(learningPathId, childId)
                .orElseThrow(() -> new RuntimeException("Learning path not found"));

        return buildLearningPathResponse(learningPath, 0, 0, 0);
    }

    /**
     * Generate lessons using AI
     */
    private List<Map<String, Object>> generateLessonsWithAI(String query, String subject, int age) {
        // First, extract the underlying concept from the question
        String concept = extractConceptFromQuery(query, subject, age);

        log.info("Extracted concept from query '{}': {}", query, concept);

        String prompt = String.format(
                "A %d-year-old student asked: '%s'\n\n" +
                "The underlying concept they need to learn is: %s\n\n" +
                "Create 3 prerequisite lessons that teach this concept step-by-step.\n\n" +
                "Each lesson should have:\n" +
                "1. A clear, engaging title about the CONCEPT (not the specific question)\n" +
                "2. A brief description (1-2 sentences)\n" +
                "3. Detailed educational content (3-5 paragraphs) that:\n" +
                "   - Explains the concept clearly with examples\n" +
                "   - Uses age-appropriate language\n" +
                "   - Includes real-world applications\n" +
                "   - Breaks down complex ideas into simple steps\n" +
                "   - Shows worked examples similar to the original question\n" +
                "4. 2-3 learning resources (videos, practice exercises, readings)\n\n" +
                "Return ONLY valid JSON array (no markdown, no code blocks, no extra text):\n" +
                "[{\n" +
                "  \"title\": \"Lesson Title\",\n" +
                "  \"description\": \"Brief description\",\n" +
                "  \"content\": \"Detailed multi-paragraph explanation with examples and real-world applications\",\n" +
                "  \"resources\": [\n" +
                "    {\"type\": \"VIDEO\", \"title\": \"Resource title\", \"description\": \"What they'll learn\"},\n" +
                "    {\"type\": \"PRACTICE\", \"title\": \"Practice title\", \"description\": \"What they'll practice\"}\n" +
                "  ]\n" +
                "}]\n\n" +
                "Resource types: VIDEO, PRACTICE, INTERACTIVE_DEMO, READING, QUIZ\n" +
                "Make the content educational, engaging, and appropriate for a %d-year-old.",
                age, query, concept, age
        );

        try {
            String response = aiProviderService.generateEducationalResponse(prompt, age, subject);

            // Clean response (remove markdown code blocks if present)
            response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            log.info("AI generated lessons response (first 200 chars): {}",
                    response.length() > 200 ? response.substring(0, 200) + "..." : response);

            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to generate lessons with AI: {}", e.getMessage(), e);
            // Return default lessons
            return getDefaultLessons(query, subject, age);
        }
    }

    /**
     * Extract the underlying concept from a student's question
     */
    private String extractConceptFromQuery(String query, String subject, int age) {
        String prompt = String.format(
                "A %d-year-old student in %s asked: '%s'\n\n" +
                "What is the underlying mathematical/scientific CONCEPT they need to learn to answer this question?\n\n" +
                "Examples:\n" +
                "- Question: 'what is 8/9 + 4/7 ?' → Concept: 'Adding fractions with different denominators'\n" +
                "- Question: 'how do plants make oxygen?' → Concept: 'Photosynthesis'\n" +
                "- Question: 'what is 15 x 23?' → Concept: 'Multi-digit multiplication'\n\n" +
                "Return ONLY the concept name (no explanation, no extra text).",
                age, subject, query
        );

        try {
            String concept = aiProviderService.generateEducationalResponse(prompt, age, subject);
            concept = concept.trim().replaceAll("^[\"']|[\"']$", ""); // Remove quotes if present

            // If the AI returns something too long or includes the original question, use a fallback
            if (concept.length() > 100 || concept.toLowerCase().contains(query.toLowerCase())) {
                log.warn("AI returned invalid concept: {}, using query as fallback", concept);
                return query;
            }

            return concept;
        } catch (Exception e) {
            log.error("Failed to extract concept from query: {}", e.getMessage());
            return query; // Fallback to original query
        }
    }

    /**
     * Parse resources from lesson data and generate real URLs
     */
    @SuppressWarnings("unchecked")
    private List<Lesson.LessonResource> parseResources(Object resourcesObj) {
        if (resourcesObj == null) return new ArrayList<>();

        try {
            List<Map<String, String>> resourceMaps = (List<Map<String, String>>) resourcesObj;
            return resourceMaps.stream()
                    .map(r -> {
                        String title = r.get("title");
                        String type = r.getOrDefault("type", "READING");
                        String url = generateResourceUrl(type, title);

                        return Lesson.LessonResource.builder()
                                .type(Lesson.LessonResource.ResourceType.valueOf(type))
                                .title(title)
                                .description(r.get("description"))
                                .url(url)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse resources: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Generate real, working URLs for educational resources
     */
    private String generateResourceUrl(String type, String title) {
        if (title == null || title.isEmpty()) {
            return null;
        }

        // URL-encode the search query
        String searchQuery = title.replace(" ", "+");

        switch (type) {
            case "VIDEO":
                // YouTube search for educational videos
                return "https://www.youtube.com/results?search_query=" + searchQuery + "+educational+for+kids";

            case "PRACTICE":
                // Khan Academy search
                return "https://www.khanacademy.org/search?page_search_query=" + searchQuery;

            case "INTERACTIVE_DEMO":
                // Google search for interactive demos
                return "https://www.google.com/search?q=" + searchQuery + "+interactive+demo+for+kids";

            case "READING":
                // Wikipedia or Simple Wikipedia
                return "https://simple.wikipedia.org/wiki/Special:Search?search=" + searchQuery;

            case "QUIZ":
                // Quizlet search
                return "https://quizlet.com/search?query=" + searchQuery + "&type=sets";

            default:
                return null;
        }
    }

    /**
     * Generate motivational message based on score
     */
    private String generateMotivationalMessage(int score) {
        if (score >= 30) {
            return "You're on the right track! Every expert was once a beginner. Let's learn these concepts together.";
        } else if (score >= 10) {
            return "Don't worry! Learning takes time. These lessons will help you build a strong foundation.";
        } else {
            return "Every journey starts with a single step. Let's start from the basics and build up your knowledge!";
        }
    }

    /**
     * Build response DTO
     */
    private LearningPathResponse buildLearningPathResponse(
            LearningPath learningPath,
            int score,
            int totalQuestions,
            int correctAnswers
    ) {
        List<LearningPathResponse.LessonDTO> lessonDTOs = learningPath.getLessons().stream()
                .map(lesson -> {
                    // Lock lessons that come after incomplete ones
                    boolean locked = false;
                    if (lesson.getDisplayOrder() > 0) {
                        Lesson previousLesson = learningPath.getLessons().get(lesson.getDisplayOrder() - 1);
                        locked = !previousLesson.getCompleted();
                    }

                    List<LearningPathResponse.ResourceDTO> resourceDTOs = lesson.getResources().stream()
                            .map(r -> LearningPathResponse.ResourceDTO.builder()
                                    .type(r.getType().name())
                                    .title(r.getTitle())
                                    .url(r.getUrl())
                                    .description(r.getDescription())
                                    .build())
                            .collect(Collectors.toList());

                    return LearningPathResponse.LessonDTO.builder()
                            .id(lesson.getId())
                            .title(lesson.getTitle())
                            .description(lesson.getDescription())
                            .content(lesson.getContent())
                            .displayOrder(lesson.getDisplayOrder())
                            .resources(resourceDTOs)
                            .completed(lesson.getCompleted())
                            .locked(locked)
                            .build();
                })
                .collect(Collectors.toList());

        return LearningPathResponse.builder()
                .id(learningPath.getId())
                .topic(learningPath.getTopic())
                .originalQuery(learningPath.getOriginalQuery())
                .score(score)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .motivationalMessage(learningPath.getMotivationalMessage())
                .lessons(lessonDTOs)
                .totalLessons(learningPath.getTotalLessons())
                .completedLessons(learningPath.getCompletedLessons())
                .progressPercentage(learningPath.getProgressPercentage())
                .proTip("Complete all " + learningPath.getTotalLessons() + " lessons, then retake the quiz to unlock your answer!")
                .build();
    }

    /**
     * Default lessons if AI fails - now with actual educational content
     */
    private List<Map<String, Object>> getDefaultLessons(String query, String subject, int age) {
        // Generate basic educational content based on the query
        String basicContent = String.format(
                "Welcome to your first lesson! Let's learn about %s.\n\n" +
                "What is this topic about?\n" +
                "%s is an important concept in %s. Understanding this will help you answer questions and solve problems related to this topic.\n\n" +
                "Why is it important?\n" +
                "Learning about %s helps you understand how things work in the real world. Many scientists, engineers, and everyday people use this knowledge in their daily lives.\n\n" +
                "Let's break it down:\n" +
                "Think of this topic as building blocks. Each piece of information you learn is like adding another block to your understanding. " +
                "By the end of these lessons, you'll have a strong foundation to build upon.\n\n" +
                "Take your time to read through the content, explore the resources, and don't hesitate to ask questions!",
                query, query, subject, query
        );

        String intermediateContent = String.format(
                "Now that you understand the basics, let's dive deeper into %s.\n\n" +
                "Key Concepts:\n" +
                "There are several important ideas you need to know about %s. Each concept builds on what you learned in the previous lesson.\n\n" +
                "How does it work?\n" +
                "Think about the steps involved. First, one thing happens, then another, and finally you get the result. " +
                "Understanding this process is crucial to mastering %s.\n\n" +
                "Real-world examples:\n" +
                "You can see %s in action all around you! Look for examples in nature, technology, or everyday situations. " +
                "When you connect what you're learning to real life, it becomes much easier to remember and understand.\n\n" +
                "Practice makes perfect:\n" +
                "The more you work with these concepts, the more comfortable you'll become. Try to explain what you've learned to someone else - " +
                "teaching is one of the best ways to learn!",
                query, query, query, query
        );

        String advancedContent = String.format(
                "Great job making it this far! Now let's put everything together about %s.\n\n" +
                "Connecting the dots:\n" +
                "Remember what you learned in the first two lessons? Now we're going to see how all those pieces fit together to give you " +
                "a complete understanding of %s.\n\n" +
                "Problem-solving:\n" +
                "When you face a question about %s, think about the steps:\n" +
                "1. What is the question asking?\n" +
                "2. What do I already know about this topic?\n" +
                "3. How can I apply what I've learned to find the answer?\n" +
                "4. Does my answer make sense?\n\n" +
                "Moving forward:\n" +
                "After completing these lessons, you'll be ready to tackle more challenging questions about %s. " +
                "Remember, learning is a journey, and every expert was once a beginner just like you!\n\n" +
                "Keep practicing, stay curious, and don't be afraid to make mistakes - they're an important part of learning!",
                query, query, query, query
        );

        return List.of(
                Map.of(
                        "title", "Understanding the Basics",
                        "description", "Let's start with the fundamental concepts you need to know",
                        "content", basicContent,
                        "resources", List.of(
                                Map.of(
                                        "type", "VIDEO",
                                        "title", "Introduction to " + subject,
                                        "description", "A beginner-friendly video explanation"
                                ),
                                Map.of(
                                        "type", "READING",
                                        "title", "Learn more about " + subject,
                                        "description", "Additional reading material to deepen your understanding"
                                )
                        )
                ),
                Map.of(
                        "title", "Building Your Knowledge",
                        "description", "Now let's dive deeper into the topic and explore key concepts",
                        "content", intermediateContent,
                        "resources", List.of(
                                Map.of(
                                        "type", "PRACTICE",
                                        "title", "Practice exercises for " + subject,
                                        "description", "Hands-on practice to reinforce what you've learned"
                                ),
                                Map.of(
                                        "type", "INTERACTIVE_DEMO",
                                        "title", "Interactive demonstration",
                                        "description", "See the concepts in action with this interactive tool"
                                )
                        )
                ),
                Map.of(
                        "title", "Putting It All Together",
                        "description", "Let's apply what you've learned and connect all the concepts",
                        "content", advancedContent,
                        "resources", List.of(
                                Map.of(
                                        "type", "QUIZ",
                                        "title", "Test your knowledge",
                                        "description", "Check your understanding with practice questions"
                                ),
                                Map.of(
                                        "type", "VIDEO",
                                        "title", "Advanced concepts in " + subject,
                                        "description", "Take your learning to the next level"
                                )
                        )
                )
        );
    }
}

