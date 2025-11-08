package com.thinkfirst.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotNull(message = "Age is required")
    @Min(value = 5, message = "Age must be at least 5")
    @Max(value = 18, message = "Age must be at most 18")
    private Integer age;
    
    private String gradeLevel; // KINDERGARTEN, FIRST, SECOND, etc.
    
    @NotNull(message = "Parent ID is required")
    private Long parentId;
}

