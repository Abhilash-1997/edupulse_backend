package com.school.management.dto.response;

import com.school.management.constant.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamResultsResponse {

    private StudentInfo student;
    private List<ExamGroup> examResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private UUID id;
        private String name;
        private String admissionNumber;
        private String className;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamGroup {
        private UUID examId;
        private String examName;
        private ExamType examType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String className;
        private UUID classId;
        private List<SubjectResult> subjects;
        private Float totalObtained;
        private Float totalMax;
        private String overallPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectResult {
        private UUID subjectId;
        private String subjectName;
        private String subjectCode;
        private Float marksObtained;
        private Float maxMarks;
        private String percentage;
        private String grade;
        private String remarks;
    }
}