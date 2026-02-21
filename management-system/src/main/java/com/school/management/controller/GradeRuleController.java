package com.school.management.controller;

import com.school.management.dto.request.CreateGradeRuleRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.GradeRuleResponse;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.service.GradeRuleService;
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
 * Grade Rule Management Controller
 * Base path: /api/grade-rules
 * Access: SCHOOL_ADMIN, SUPER_ADMIN only
 */
@Slf4j
@RestController
@RequestMapping("/grade-rules")
@RequiredArgsConstructor
@RequireAdmin
public class GradeRuleController {

    private final GradeRuleService gradeRuleService;

    /**
     * Create grade rule
     * POST /api/grade-rules
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GradeRuleResponse>> createGradeRule(
            @Valid @RequestBody CreateGradeRuleRequest request) {

        log.info("Creating grade rule: {}", request.getGrade());

        GradeRuleResponse gradeRule = gradeRuleService.createGradeRule(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grade rule created successfully", gradeRule));
    }

    /**
     * Bulk create grade rules
     * POST /api/grade-rules/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<GradeRuleResponse>>> createGradeRules(
            @Valid @RequestBody List<CreateGradeRuleRequest> requests) {

        log.info("Bulk creating {} grade rules", requests.size());

        List<GradeRuleResponse> gradeRules = gradeRuleService.createGradeRules(requests);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithCount("Grade rules created successfully", gradeRules, gradeRules.size()));
    }

    /**
     * Get all grade rules
     * GET /api/grade-rules
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GradeRuleResponse>>> getGradeRules() {
        log.info("Fetching all grade rules");

        List<GradeRuleResponse> gradeRules = gradeRuleService.getGradeRules();

        return ResponseEntity.ok(
                ApiResponse.successWithCount("Grade rules retrieved successfully", gradeRules, gradeRules.size()));
    }

    /**
     * Get grade rule by ID
     * GET /api/grade-rules/:id
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GradeRuleResponse>> getGradeRuleById(@PathVariable UUID id) {
        log.info("Fetching grade rule by ID: {}", id);

        GradeRuleResponse gradeRule = gradeRuleService.getGradeRuleById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Grade rule retrieved successfully", gradeRule));
    }

    /**
     * Update grade rule
     * PUT /api/grade-rules/:id
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GradeRuleResponse>> updateGradeRule(
            @PathVariable UUID id,
            @Valid @RequestBody CreateGradeRuleRequest request) {

        log.info("Updating grade rule ID: {}", id);
        GradeRuleResponse gradeRule = gradeRuleService.updateGradeRule(id, request);
        return ResponseEntity.ok(ApiResponse.success("Grade rule updated successfully", gradeRule));
    }

    /**
     * Delete grade rule
     * DELETE /api/grade-rules/:id
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGradeRule(@PathVariable UUID id) {
        log.info("Deleting grade rule ID: {}", id);

        gradeRuleService.deleteGradeRule(id);

        return ResponseEntity.ok(
                ApiResponse.success("Grade rule deleted successfully", null));
    }

    /**
     * Calculate grade for a percentage
     * GET /api/grade-rules/calculate?percentage=85.5
     */
    @GetMapping("/calculate")
    public ResponseEntity<ApiResponse<String>> calculateGrade(@RequestParam Float percentage) {
        log.info("Calculating grade for percentage: {}", percentage);

        String grade = gradeRuleService.calculateGrade(percentage);

        return ResponseEntity.ok(
                ApiResponse.success("Grade calculated successfully", grade));
    }
}