package com.school.management.controller;

import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.BulkUploadResponse;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.security.annotation.RequireTeacher;
import com.school.management.service.BulkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/bulk-upload")
@RequiredArgsConstructor
public class BulkUploadController {

    private final BulkUploadService bulkUploadService;

    // ========================= STUDENTS + PARENTS =========================

    @PostMapping("/students")
    @RequireAdmin
    public ResponseEntity<ApiResponse<BulkUploadResponse>> uploadStudents(
            @RequestParam("file") MultipartFile file) {
        BulkUploadResponse response = bulkUploadService.uploadStudentParent(file);
        return ResponseEntity.ok(ApiResponse.success("Students uploaded successfully", response));
    }

    // ========================= ATTENDANCE =========================

    @PostMapping("/attendance")
    @RequireTeacher
    public ResponseEntity<ApiResponse<BulkUploadResponse>> uploadAttendance(
            @RequestParam("file") MultipartFile file) {
        BulkUploadResponse response = bulkUploadService.uploadAttendance(file);
        return ResponseEntity.ok(ApiResponse.success("Attendance uploaded successfully", response));
    }

    // ========================= EXAMS =========================

    @PostMapping("/exams")
    @RequireAdmin
    public ResponseEntity<ApiResponse<BulkUploadResponse>> uploadExams(
            @RequestParam("file") MultipartFile file) {
        BulkUploadResponse response = bulkUploadService.uploadExams(file);
        return ResponseEntity.ok(ApiResponse.success("Exams uploaded successfully", response));
    }

    // ========================= EXAM RESULTS =========================

    @PostMapping("/results")
    @RequireAdmin
    public ResponseEntity<ApiResponse<BulkUploadResponse>> uploadExamResults(
            @RequestParam("file") MultipartFile file) {
        BulkUploadResponse response = bulkUploadService.uploadExamResults(file);
        return ResponseEntity.ok(ApiResponse.success("Exam results uploaded successfully", response));
    }

    // ========================= LIBRARY SECTIONS =========================

    @PostMapping("/library-sections")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<BulkUploadResponse>> uploadLibrarySections(
            @RequestParam("file") MultipartFile file) {
        BulkUploadResponse response = bulkUploadService.uploadLibrarySections(file);
        return ResponseEntity.ok(ApiResponse.success("Library sections uploaded successfully", response));
    }

    // ========================= BOOKS =========================

    @PostMapping("/books")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<BulkUploadResponse>> uploadBooks(
            @RequestParam("file") MultipartFile file) {
        BulkUploadResponse response = bulkUploadService.uploadBooks(file);
        return ResponseEntity.ok(ApiResponse.success("Books uploaded successfully", response));
    }
}
