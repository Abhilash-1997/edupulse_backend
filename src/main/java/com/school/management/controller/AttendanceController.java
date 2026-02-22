package com.school.management.controller;

import com.school.management.dto.request.MarkAttendanceRequest;
import com.school.management.dto.request.UpdateAttendanceRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.AttendanceResponse;
import com.school.management.dto.response.AttendanceStatusResponse;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.security.annotation.RequireAdminOrTeacher;
import com.school.management.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Attendance Management Controller
 * Base path: /api/attendance
 * All routes require authentication (protect middleware in Express)
 */
@Slf4j
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * Mark attendance for students (bulk operation)
     * POST /api/attendance/mark
     * Access: TEACHER, SCHOOL_ADMIN
     */
    @PostMapping("/mark")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> markAttendance(
            @Valid @RequestBody MarkAttendanceRequest request) {

        log.info("Marking attendance for section: {} on date: {}",
                request.getSectionId(), request.getDate());

        List<AttendanceResponse> responses = attendanceService.markAttendance(request);

        return ResponseEntity.ok(
                ApiResponse.success("Attendance marked successfully", responses)
        );
    }

    /**
     * Get attendance status (marked and pending)
     * GET /api/attendance?sectionId=uuid&date=2024-02-15
     * Access: All authenticated users
     */
    @GetMapping
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<AttendanceStatusResponse>> getAttendance(
            @RequestParam UUID sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Fetching attendance for section: {} on date: {}", sectionId, date);

        AttendanceStatusResponse response = attendanceService.getAttendance(sectionId, date);

        return ResponseEntity.ok(
                ApiResponse.success("Attendance fetched successfully", response)
        );
    }

    /**
     * Get attendance report with filters
     * GET /api/attendance/report?sectionId=uuid&date=2024-02-15
     * GET /api/attendance/report?studentId=uuid&startDate=2024-02-01&endDate=2024-02-15
     * Access: All authenticated users
     */
    @GetMapping("/report")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceReport(
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Fetching attendance report - sectionId: {}, studentId: {}, date: {}, startDate: {}, endDate: {}",
                sectionId, studentId, date, startDate, endDate);
        List<AttendanceResponse> attendance = attendanceService.getAttendanceReport(sectionId, studentId, date, startDate, endDate);
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Attendance report fetched successfully",
                        attendance, attendance.size())
        );
    }

    /**
     * Update single attendance record
     * PUT /api/attendance/:id
     * Access: All authenticated users
     */
    @PutMapping("/{id}")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendance(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        log.info("Updating attendance record ID: {} with status: {}", id, request.getStatus());
        AttendanceResponse response = attendanceService.updateAttendance(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Attendance updated successfully", response)
        );
    }

    /**
     * Lock attendance for a section and date (prevents further modifications)
     * POST /api/attendance/lock
     * Access: SCHOOL_ADMIN
     *
     * Optional endpoint (not in original Express routes)
     */
    @PostMapping("/lock")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> lockAttendance(
            @RequestParam UUID sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Locking attendance for section: {} on date: {}", sectionId, date);
        attendanceService.lockAttendance(sectionId, date);
        return ResponseEntity.ok(ApiResponse.success("Attendance locked successfully", null)
        );
    }
}