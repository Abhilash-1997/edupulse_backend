package com.school.management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddExamResultRequest {

    @NotNull(message = "Exam ID is required")
    private UUID examId;

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotEmpty(message = "Results cannot be empty")
    @Valid
    private List<StudentResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentResult {

        @NotNull(message = "Student ID is required")
        private UUID studentId;

        @NotNull(message = "Marks obtained is required")
        @Min(value = 0, message = "Marks cannot be negative")
        private Float marksObtained;

        private Float maxMarks;
        private String grade;
        private String remarks;
    }
}