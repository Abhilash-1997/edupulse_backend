package com.school.management.controller;

import com.school.management.dto.request.UpdateSchoolStatusRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.SchoolInfoResponse;
import com.school.management.dto.response.SchoolStatsResponse;
import com.school.management.dto.response.SystemStatsResponse;
import com.school.management.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Dashboard Controller
 * Base path: /dashboard
 * Provides school-level and system-level statistics
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ======================== SCHOOL ADMIN ========================

    /**
     * Get school dashboard statistics (students, teachers, classes, parents +
     * recent students)
     * GET /dashboard/stats
     * Accessible by any authenticated user (school-scoped)
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SchoolStatsResponse>> getSchoolStats() {
        log.info("Request: GET /dashboard/stats");
        SchoolStatsResponse response = dashboardService.getSchoolStats();
        return ResponseEntity.ok(
                ApiResponse.success("School stats retrieved successfully", response));
    }

    // ======================== SUPER ADMIN ========================

    /**
     * Get system-wide statistics (schools, users, revenue + recent schools)
     * GET /dashboard/system-stats
     * Restricted to SUPER_ADMIN role
     */
    @GetMapping("/system-stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SystemStatsResponse>> getSystemStats() {
        log.info("Request: GET /dashboard/system-stats");
        SystemStatsResponse response = dashboardService.getSystemStats();
        return ResponseEntity.ok(
                ApiResponse.success("System stats retrieved successfully", response));
    }

    /**
     * Get all schools with aggregated counts
     * GET /dashboard/schools
     * Restricted to SUPER_ADMIN role
     */
    @GetMapping("/schools")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SchoolInfoResponse>>> getSchools() {
        log.info("Request: GET /dashboard/schools");
        List<SchoolInfoResponse> response = dashboardService.getSchools();
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Schools retrieved successfully", response, response.size()));
    }

    /**
     * Update school status (PENDING → ACTIVE, INACTIVE, etc.)
     * PUT /dashboard/schools/{schoolId}/status
     * Restricted to SUPER_ADMIN role
     */
    @PutMapping("/schools/{schoolId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolInfoResponse>> updateSchoolStatus(
            @PathVariable UUID schoolId,
            @Valid @RequestBody UpdateSchoolStatusRequest request) {
        log.info("Request: PUT /dashboard/schools/{}/status -> {}", schoolId, request.getStatus());
        SchoolInfoResponse response = dashboardService.updateSchoolStatus(schoolId, request.getStatus());
        return ResponseEntity.ok(
                ApiResponse.success("School status updated successfully", response));
    }
}
