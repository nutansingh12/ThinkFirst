package com.thinkfirst.controller;

import com.thinkfirst.dto.ChildRequest;
import com.thinkfirst.dto.ChildResponse;
import com.thinkfirst.model.Child;
import com.thinkfirst.service.ChildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
@Tag(name = "Child Management", description = "Manage child profiles")
public class ChildController {

    private final ChildService childService;

    @PostMapping
    @Operation(summary = "Create a new child profile")
    public ResponseEntity<ChildResponse> createChild(@Valid @RequestBody ChildRequest request) {
        return ResponseEntity.ok(childService.createChild(request));
    }

    @GetMapping("/parent/{parentId}")
    @Operation(summary = "Get all children for a parent")
    public ResponseEntity<List<ChildResponse>> getParentChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(childService.getParentChildren(parentId));
    }

    @GetMapping("/{childId}")
    @Operation(summary = "Get child by ID")
    public ResponseEntity<ChildResponse> getChild(@PathVariable Long childId) {
        return ResponseEntity.ok(childService.getChild(childId));
    }

    @PutMapping("/{childId}")
    @Operation(summary = "Update child profile")
    public ResponseEntity<ChildResponse> updateChild(
            @PathVariable Long childId,
            @Valid @RequestBody ChildRequest request) {
        return ResponseEntity.ok(childService.updateChild(childId, request));
    }

    @DeleteMapping("/{childId}")
    @Operation(summary = "Delete child profile")
    public ResponseEntity<Void> deleteChild(@PathVariable Long childId) {
        childService.deleteChild(childId);
        return ResponseEntity.noContent().build();
    }
}

