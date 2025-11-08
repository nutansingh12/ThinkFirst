package com.thinkfirst.controller;

import com.thinkfirst.dto.QuizResult;
import com.thinkfirst.dto.QuizSubmission;
import com.thinkfirst.model.Quiz;
import com.thinkfirst.service.QuizService;
import com.thinkfirst.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@Tag(name = "Quiz", description = "Quiz management and submission endpoints")
public class QuizController {

    private final QuizService quizService;
    private final RateLimitService rateLimitService;

    public QuizController(QuizService quizService, RateLimitService rateLimitService) {
        this.quizService = quizService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit quiz answers for evaluation")
    public ResponseEntity<QuizResult> submitQuiz(@Valid @RequestBody QuizSubmission submission) {
        // Check quiz submission rate limit
        rateLimitService.checkQuizRateLimit(submission.getChildId());

        return ResponseEntity.ok(quizService.evaluateQuiz(submission));
    }
    
    @GetMapping("/{quizId}")
    @Operation(summary = "Get quiz by ID")
    public ResponseEntity<Quiz> getQuiz(@PathVariable Long quizId) {
        Quiz quiz = quizService.quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        return ResponseEntity.ok(quiz);
    }
}

