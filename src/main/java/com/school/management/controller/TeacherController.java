package com.school.management.controller;

import com.school.management.dto.response.*;
import com.school.management.security.annotation.RequireTeacher;
import com.school.management.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Teacher Controller
 * Base path: /teacher
 * Provides teacher-specific views: assigned class, students, periods,
 * timetable, and ID card generation.
 */
@Slf4j
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    // ======================== MY CLASS ========================

    /**
     * Get class assigned to the authenticated teacher as Class Teacher.
     * GET /teacher/my-class
     */
    @GetMapping("/my-class")
    @RequireTeacher
    public ResponseEntity<ApiResponse<MyClassResponse>> getMyClass() {
        log.info("Request: GET /teacher/my-class");
        MyClassResponse response = teacherService.getMyClass();
        return ResponseEntity.ok(
                ApiResponse.success("Class details fetched successfully", response));
    }

    // ======================== MY STUDENTS ========================

    /**
     * Get students of the section assigned to the authenticated teacher.
     * GET /teacher/my-students
     */
    @GetMapping("/my-students")
    @RequireTeacher
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getMyStudents() {
        log.info("Request: GET /teacher/my-students");
        List<StudentResponse> response = teacherService.getMyStudents();
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Students fetched successfully", response, response.size()));
    }

    // ======================== ID CARD ========================

    /**
     * Generate ID card data for a specific student.
     * Accessible by TEACHER (class teacher only), PARENT (own child), and
     * SCHOOL_ADMIN.
     * GET /teacher/student/{studentId}/id-card
     */
    @GetMapping("/student/{studentId}/id-card")
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<IDCardDataResponse>> getIDCardData(
            @PathVariable UUID studentId) {
        log.info("Request: GET /teacher/student/{}/id-card", studentId);
        IDCardDataResponse response = teacherService.getIDCardData(studentId);
        return ResponseEntity.ok(
                ApiResponse.success("ID Card data generated", response));
    }

    // ======================== MY PERIODS ========================

    /**
     * Get all timetable periods assigned to the authenticated teacher.
     * GET /teacher/my-periods
     */
    @GetMapping("/my-periods")
    @RequireTeacher
    public ResponseEntity<ApiResponse<List<TimetableResponse>>> getMyPeriods() {
        log.info("Request: GET /teacher/my-periods");
        List<TimetableResponse> response = teacherService.getMyPeriods();
        return ResponseEntity.ok(
                ApiResponse.success("Periods fetched successfully", response));
    }

    // ======================== MY CLASS TIMETABLE ========================

    /**
     * Get timetable for the section managed by the authenticated teacher.
     * GET /teacher/my-class-timetable
     */
    @GetMapping("/my-class-timetable")
    @RequireTeacher
    public ResponseEntity<ApiResponse<MyClassTimetableResponse>> getMyClassTimetable() {
        log.info("Request: GET /teacher/my-class-timetable");
        MyClassTimetableResponse response = teacherService.getMyClassTimetable();
        return ResponseEntity.ok(
                ApiResponse.success("Class timetable fetched successfully", response));
    }
}
