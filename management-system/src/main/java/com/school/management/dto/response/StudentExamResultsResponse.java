package com.school.management.dto.response;

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
public class StudentExamResultsResponse {

    private UUID examId;
    private String examName;
    private List<SubjectResult> subjects;
    private String overallPercentage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectResult {
        private String subjectName;
        private Float marksObtained;
        private Float maxMarks;
        private String percentage;
    }
}