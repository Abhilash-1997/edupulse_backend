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
public class ExamResultsByStudentResponse {

    private StudentInfo student;
    private Float totalObtained;
    private Float totalMax;
    private Float percentage;
    private Boolean isFailed;
    private List<SubjectResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private UUID id;
        private String name;
        private String admissionNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectResult {
        private SubjectInfo subject;
        private Float marksObtained;
        private Float maxMarks;
        private String grade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectInfo {
        private UUID id;
        private String name;
        private String code;
    }
}
