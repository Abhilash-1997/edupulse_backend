package com.school.management.dto.request;

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
public class CreateGradeRuleRequest {

    @NotBlank(message = "Grade is required")
    private String grade;

    @NotNull(message = "Minimum percentage is required")
    @Min(value = 0, message = "Minimum percentage cannot be negative")
    @Max(value = 100, message = "Minimum percentage cannot exceed 100")
    private Float minPercentage;

    @NotNull(message = "Maximum percentage is required")
    @Min(value = 0, message = "Maximum percentage cannot be negative")
    @Max(value = 100, message = "Maximum percentage cannot exceed 100")
    private Float maxPercentage;

    private String description;
}