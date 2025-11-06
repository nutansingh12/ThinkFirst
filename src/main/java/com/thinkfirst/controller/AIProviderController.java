package com.thinkfirst.controller;

import com.thinkfirst.service.ai.AIProviderService;
import com.thinkfirst.service.cache.AICacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-provider")
@RequiredArgsConstructor
@Tag(name = "AI Provider Management", description = "Manage AI providers and models")
public class AIProviderController {
    
    private final AIProviderService aiProviderService;
    
    @GetMapping("/status")
    @Operation(summary = "Get status of all AI providers")
    public ResponseEntity<Map<String, AIProviderService.ProviderStatus>> getProviderStatus() {
        return ResponseEntity.ok(aiProviderService.getProviderStatus());
    }
    
    @PostMapping("/test/{provider}")
    @Operation(summary = "Test a specific AI provider")
    public ResponseEntity<Map<String, Object>> testProvider(@PathVariable String provider) {
        boolean success = aiProviderService.testProvider(provider);
        return ResponseEntity.ok(Map.of(
            "provider", provider,
            "success", success,
            "message", success ? "Provider is working" : "Provider test failed"
        ));
    }
    
    @PostMapping("/openai/model")
    @Operation(summary = "Change OpenAI model")
    public ResponseEntity<Map<String, String>> setOpenAIModel(@RequestParam String modelKey) {
        aiProviderService.setOpenAIModel(modelKey);
        String currentModel = aiProviderService.getCurrentOpenAIModel();
        return ResponseEntity.ok(Map.of(
            "message", "Model updated successfully",
            "currentModel", currentModel,
            "modelKey", modelKey
        ));
    }
    
    @GetMapping("/openai/model")
    @Operation(summary = "Get current OpenAI model")
    public ResponseEntity<Map<String, String>> getCurrentOpenAIModel() {
        String currentModel = aiProviderService.getCurrentOpenAIModel();
        return ResponseEntity.ok(Map.of(
            "currentModel", currentModel
        ));
    }

    @GetMapping("/cache/stats")
    @Operation(summary = "Get cache statistics")
    public ResponseEntity<AICacheService.CacheStats> getCacheStats() {
        return ResponseEntity.ok(aiProviderService.getCacheStats());
    }

    @DeleteMapping("/cache/quiz")
    @Operation(summary = "Invalidate all quiz caches")
    public ResponseEntity<Map<String, String>> invalidateQuizCache() {
        aiProviderService.invalidateQuizCache();
        return ResponseEntity.ok(Map.of(
            "message", "Quiz cache invalidated successfully"
        ));
    }

    @DeleteMapping("/cache/response")
    @Operation(summary = "Invalidate all response caches")
    public ResponseEntity<Map<String, String>> invalidateResponseCache() {
        aiProviderService.invalidateResponseCache();
        return ResponseEntity.ok(Map.of(
            "message", "Response cache invalidated successfully"
        ));
    }
}

