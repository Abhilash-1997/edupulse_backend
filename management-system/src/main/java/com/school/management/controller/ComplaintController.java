package com.school.management.controller;

import com.school.management.constant.ComplaintPriority;
import com.school.management.constant.ComplaintStatus;
import com.school.management.dto.request.CreateComplaintRequest;
import com.school.management.dto.request.UpdateComplaintStatusRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.ComplaintResponse;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.security.annotation.RequireParent;
import com.school.management.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Complaint Management Controller
 * Base path: /complaints
 */
@Slf4j
@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    // ======================== PARENT OPERATIONS ========================

    /**
     * Create a new complaint (Parent only)
     * POST /complaints
     */
    @PostMapping("/request")
    @RequireParent
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request) {

        log.info("Creating complaint: {}", request.getTitle());

        ComplaintResponse response = complaintService.createComplaint(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Complaint created successfully", response));
    }

    /**
     * Get complaints filed by the current parent (with optional filters)
     * GET /complaints/my?status=...&priority=...
     */
    @GetMapping("/my-complaints")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getMyComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintPriority priority) {

        log.info("Fetching my complaints, status: {}, priority: {}", status, priority);

        List<ComplaintResponse> response = complaintService.myComplaints(status, priority);

        return ResponseEntity.ok(
                ApiResponse.successWithCount("My complaints retrieved successfully", response, response.size()));
    }

    // ======================== ADMIN / TEACHER OPERATIONS ========================

    /**
     * Get all complaints with optional filters (Admin / Teacher)
     * GET /complaints?status=...&priority=...
     */
    @GetMapping("/")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintPriority priority) {

        log.info("Fetching complaints, status: {}, priority: {}", status, priority);

        List<ComplaintResponse> response = complaintService.getComplaints(status, priority);

        return ResponseEntity.ok(
                ApiResponse.successWithCount("Complaints retrieved successfully", response, response.size()));
    }

    /**
     * Get complaint by ID (Admin or complaint owner)
     * GET /complaints/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getComplaintById(@PathVariable UUID id) {
        log.info("Fetching complaint by ID: {}", id);
        ComplaintResponse response = complaintService.getComplaintById(id);

        return ResponseEntity.ok(ApiResponse.success("Complaint retrieved successfully", response));
    }

    /**
     * Update complaint status (Admin only)
     * PUT /complaints/{id}/status
     */
    @PutMapping("/{id}/status")
    @RequireAdmin
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateComplaintStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplaintStatusRequest request) {

        log.info("Updating complaint {} status to: {}", id, request.getStatus());

        ComplaintResponse response = complaintService.updateComplaintStatus(id, request.getStatus());

        return ResponseEntity.ok(ApiResponse.success("Complaint status updated successfully", response));
    }

    /**
     * Delete complaint (Admin only)
     * DELETE /complaints/{id}
     */
    @DeleteMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> deleteComplaint(@PathVariable UUID id) {

        log.info("Deleting complaint: {}", id);

        complaintService.deleteComplaint(id);

        return ResponseEntity.ok(ApiResponse.success("Complaint deleted successfully", null));
    }
}
