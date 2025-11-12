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
        String prompt = String.format(
                "For a %d-year-old who needs to learn about '%s' in %s, create 3 prerequisite lessons. " +
                "Return ONLY valid JSON array (no markdown, no code blocks):\n" +
                "[{\"title\":\"Lesson Title\",\"description\":\"Brief description\",\"content\":\"Detailed explanation\",\"resources\":[{\"type\":\"VIDEO\",\"title\":\"Resource title\",\"description\":\"What they'll learn\"}]}]\n" +
                "Resource types: VIDEO, PRACTICE, INTERACTIVE_DEMO, READING, QUIZ",
                age, query, subject
        );

        try {
            String response = aiProviderService.generateEducationalResponse(prompt, age, subject);
            
            // Clean response (remove markdown code blocks if present)
            response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to generate lessons with AI: {}", e.getMessage());
            // Return default lessons
            return getDefaultLessons(query);
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
     * Default lessons if AI fails
     */
    private List<Map<String, Object>> getDefaultLessons(String query) {
        return List.of(
                Map.of(
                        "title", "Understanding the Basics",
                        "description", "Let's start with the fundamental concepts",
                        "content", "This lesson covers the basic concepts you need to understand " + query,
                        "resources", List.of()
                ),
                Map.of(
                        "title", "Building Your Knowledge",
                        "description", "Now let's dive deeper into the topic",
                        "content", "In this lesson, we'll explore " + query + " in more detail",
                        "resources", List.of()
                ),
                Map.of(
                        "title", "Putting It All Together",
                        "description", "Let's apply what you've learned",
                        "content", "This final lesson helps you connect all the concepts about " + query,
                        "resources", List.of()
                )
        );
    }
}

