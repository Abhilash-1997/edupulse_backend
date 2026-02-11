package com.school.management.controller;

import com.school.management.dto.request.CollectFeeRequest;
import com.school.management.dto.request.CreateFeeStructureRequest;
import com.school.management.dto.request.ProcessPaymentRequest;
import com.school.management.dto.response.*;
import com.school.management.entity.FeePayment;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.FeePaymentRepository;
import com.school.management.service.FinanceService;
import com.school.management.service.PdfGenerationService;
import com.school.management.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Finance Management Controller
 * Base path: /finance
 * Maps all Express finance routes
 */
@Slf4j
@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final PdfGenerationService pdfGeneratorService;
    private final FeePaymentRepository feePaymentRepository;

    // ======================== FEE STRUCTURE ========================

    /**
     * Create a new fee structure
     * POST /finance/fees
     * Restricted to fee collection access (Admin / Accounts staff)
     */
    @PostMapping("/fees")
    @PreAuthorize("@financeAccess.hasAccess()")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> createFeeStructure(
            @Valid @RequestBody CreateFeeStructureRequest request) {

        log.info("Creating fee structure: {}", request.getName());

        FeeStructureResponse response = financeService.createFeeStructure(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee structure created successfully", response));
    }

    /**
     * Get all fee structures (optionally filtered by classId)
     * GET /finance/fees?classId=...
     * Accessible by any authenticated user
     */
    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<List<FeeStructureResponse>>> getFeeStructures(
            @RequestParam(required = false) UUID classId) {

        log.info("Fetching fee structures, classId: {}", classId);

        List<FeeStructureResponse> response = financeService.getFeeStructures(classId);

        return ResponseEntity.ok(
                ApiResponse.successWithCount("Fee structures retrieved successfully", response, response.size()));
    }

    /**
     * Collect fee for a student
     * POST /finance/fees/collect
     * Restricted to fee collection access
     */
    @PostMapping("/fees/collect")
    @PreAuthorize("@financeAccess.hasAccess()")
    public ResponseEntity<ApiResponse<FeePaymentResponse>> collectFee(
            @Valid @RequestBody CollectFeeRequest request) {

        log.info("Collecting fee for student: {}", request.getStudentId());

        FeePaymentResponse response = financeService.collectFee(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee collected successfully", response));
    }

    /**
     * Generate fee receipt PDF
     * GET /finance/receipt/{paymentId}
     * Restricted to fee collection access
     */
    @GetMapping("/receipt/{paymentId}")
    @PreAuthorize("@financeAccess.hasAccess()")
    public ResponseEntity<ByteArrayResource> generateFeeReceipt(@PathVariable UUID paymentId) {
        log.info("Generating fee receipt for payment ID: {}", paymentId);

        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        FeePayment feePayment =
                feePaymentRepository.findForReceipt(paymentId, schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        byte[] pdfBytes = pdfGeneratorService.generateFeeReceipt(feePayment);

        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        String filename = String.format("FeeReceipt_%s_%s.pdf",
                feePayment.getStudent().getAdmissionNumber(),
                feePayment.getPaymentDate());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    /**
     * Get all receipts (optionally filtered by studentId)
     * GET /finance/receipts?studentId=...&page=0&size=20
     * Restricted to fee collection access
     */
    @GetMapping("/receipts")
    @PreAuthorize("@financeAccess.hasAccess()")
    public ResponseEntity<ApiResponse<Page<FeePaymentResponse>>> getReceipts(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        log.info("Fetching receipts, studentId: {}", studentId);

        Page<FeePaymentResponse> response = financeService.getReceipts(studentId, page, size);

        return ResponseEntity.ok(ApiResponse.success("Receipts retrieved successfully", response));
    }

    // ======================== STATISTICS & TRACKING ========================

    /**
     * Get fee statistics overview
     * GET /finance/statistics
     * Restricted to fee collection access
     */
    @GetMapping("/statistics")
    @PreAuthorize("@financeAccess.hasAccess()")
    public ResponseEntity<ApiResponse<FeeStatisticsResponse>> getFeeStatistics() {
        log.info("Fetching fee statistics");

        FeeStatisticsResponse response = financeService.getFeeStatistics();

        return ResponseEntity.ok(ApiResponse.success("Fee statistics retrieved successfully", response));
    }

    /**
     * Get fee status of all students in a class
     * GET /finance/class/{classId}/students
     * Restricted to fee collection access
     */
    @GetMapping("/class/{classId}/students")
    @PreAuthorize("@financeAccess.hasAccess()")
    public ResponseEntity<ApiResponse<List<StudentFeeStatusResponse>>> getClassFeeStatus(
            @PathVariable UUID classId) {

        log.info("Fetching class fee status for classId: {}", classId);

        List<StudentFeeStatusResponse> response = financeService.getClassFeeStatus(classId);

        return ResponseEntity.ok(
                ApiResponse.successWithCount("Class fee status retrieved successfully", response, response.size()));
    }

    /**
     * Get student fee details (breakdown + payment history)
     * GET /finance/student/{studentId}/fees
     * Accessible by admin and parent
     */
    @GetMapping("/student/{studentId}/fees")
    public ResponseEntity<ApiResponse<StudentFeeDetailsResponse>> getStudentFeeDetails(
            @PathVariable UUID studentId) {

        log.info("Fetching student fee details for studentId: {}", studentId);

        StudentFeeDetailsResponse response = financeService.getStudentFeeDetails(studentId);

        return ResponseEntity.ok(
                ApiResponse.success("Student fee details fetched successfully", response));
    }

    // ======================== PARENT PAYMENT ========================

    /**
     * Process a fee payment (parent-initiated)
     * POST /finance/payment
     * Restricted to PARENT role
     */
    @PostMapping("/payment")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<FeePaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {

        log.info("Processing parent payment for student: {}", request.getStudentId());

        FeePaymentResponse response = financeService.processPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", response));
    }
}
