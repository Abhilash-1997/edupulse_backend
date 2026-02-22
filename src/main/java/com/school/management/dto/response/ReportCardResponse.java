package com.school.management.dto.response;

import com.school.management.constant.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCardResponse {

    private UUID id;
    private UUID examId;
    private UUID studentId;
    private UUID subjectId;
    private Float marksObtained;
    private Float maxMarks;
    private String grade;
    private String remarks;

    // Subject info
    private String subjectName;
    private String subjectCode;

    // Exam info
    private String examName;
    private ExamType examType;
}
