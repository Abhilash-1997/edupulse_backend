package com.school.management.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExamResultRequest {

    @Min(value = 0, message = "Marks cannot be negative")
    private Float marksObtained;

    private Float maxMarks;
    private String grade;
    private String remarks;
}