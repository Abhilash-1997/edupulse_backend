package com.school.management.controller;

import com.school.management.dto.request.AddExamResultRequest;
import com.school.management.dto.request.CreateExamRequest;
import com.school.management.dto.request.UpdateExamResultRequest;
import com.school.management.dto.response.*;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.security.annotation.RequireAdminOrTeacher;
import com.school.management.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

        private final ExamService examService;

        // ============= EXAM MANAGEMENT ENDPOINTS =============

        @PostMapping
        @RequireAdmin
        public ResponseEntity<ApiResponse<ExamResponse>> createExam(
                        @Valid @RequestBody CreateExamRequest request) {
                ExamResponse response = examService.createExam(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Exam created successfully", response));
        }

        @GetMapping
        @RequireAdminOrTeacher
        public ResponseEntity<ApiResponse<List<ExamResponse>>> getExams(
                        @RequestParam(required = false) UUID classId,
                        @RequestParam(required = false) UUID sectionId) {
                List<ExamResponse> exams = examService.getAllExams(classId, sectionId);
                return ResponseEntity.ok(ApiResponse.successWithCount("Exams fetched successfully",
                                exams, exams.size()));
        }

        // ============= RESULTS MANAGEMENT ENDPOINTS =============

        @PostMapping("/results")
        @RequireAdminOrTeacher
        public ResponseEntity<ApiResponse<List<ExamResultResponse>>> addExamResult(
                        @Valid @RequestBody AddExamResultRequest request) {
                List<ExamResultResponse> responses = examService.addExamResults(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Exam results added successfully", responses));
        }

        @PutMapping("/results/{id}")
        @RequireAdminOrTeacher
        public ResponseEntity<ApiResponse<ExamResultResponse>> updateExamResult(
                        @PathVariable UUID id,
                        @Valid @RequestBody UpdateExamResultRequest request) {
                ExamResultResponse response = examService.updateExamResult(id, request);
                return ResponseEntity.ok(ApiResponse.success("Exam result updated successfully", response));
        }

        @GetMapping("/results")
        @RequireAdminOrTeacher
        public ResponseEntity<ApiResponse<List<ExamResultsByStudentResponse>>> getExamResults(
                        @RequestParam(required = false) UUID examId,
                        @RequestParam(required = false) UUID subjectId,
                        @RequestParam(required = false) UUID classId,
                        @RequestParam(required = false) UUID sectionId) {
                List<ExamResultsByStudentResponse> results = examService.getExamResults(
                                examId, subjectId, classId, sectionId);
                return ResponseEntity.ok(ApiResponse.successWithCount("Exam results fetched successfully",
                                results, results.size()));
        }

        @GetMapping("/student-results")
        public ResponseEntity<ApiResponse<StudentExamResultsResponse>> getStudentExamResults(
                        @RequestParam UUID studentId) {
                StudentExamResultsResponse response = examService.getStudentExamResults(studentId);
                return ResponseEntity.ok(ApiResponse.successWithCount(
                                "Student exam results fetched successfully",
                                response,
                                response.getExamResults().size()));
        }

        @GetMapping("/report")
        public ResponseEntity<ApiResponse<List<ReportCardResponse>>> getStudentReportCard(
                        @RequestParam UUID studentId,
                        @RequestParam(required = false) UUID examId) {
                List<ReportCardResponse> report = examService.getStudentReportCard(studentId, examId);
                return ResponseEntity.ok(ApiResponse.successWithCount(
                                "Report card fetched successfully",
                                report,
                                report.size()));
        }

        @GetMapping("/report/download")
        public ResponseEntity<byte[]> downloadReportCard(
                        @RequestParam UUID studentId,
                        @RequestParam UUID examId) {
                byte[] pdfBytes = examService.downloadReportCard(studentId, examId);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentLength(pdfBytes.length);
                headers.setContentDispositionFormData("attachment",
                                "ReportCard_" + studentId + ".pdf");

                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        }
}
