package com.thinkfirst.controller;

import com.thinkfirst.dto.QuizResult;
import com.thinkfirst.dto.QuizSubmission;
import com.thinkfirst.model.Quiz;
import com.thinkfirst.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "Quiz management and submission endpoints")
public class QuizController {
    
    private final QuizService quizService;
    
    @PostMapping("/submit")
    @Operation(summary = "Submit quiz answers for evaluation")
    public ResponseEntity<QuizResult> submitQuiz(@Valid @RequestBody QuizSubmission submission) {
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

