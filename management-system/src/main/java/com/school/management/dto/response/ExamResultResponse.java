package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultResponse {

    private UUID id;
    private Float marksObtained;
    private Float maxMarks;
    private String grade;
    private String remarks;
    private Float percentage;
    private UUID examId;
    private UUID studentId;
    private UUID subjectId;
    private String subjectName;
}