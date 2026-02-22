package com.school.management.controller;

import com.school.management.dto.request.CreatePayrollRequest;
import com.school.management.dto.request.GeneratePayrollRequest;  // ← Add this import
import com.school.management.dto.request.UpsertSalaryStructureRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.PayrollResponse;
import com.school.management.dto.response.SalaryStructureResponse;
import com.school.management.entity.StaffProfile;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.PayrollRepository;
import com.school.management.repository.StaffProfileRepository;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.service.PayrollService;
import com.school.management.service.PdfGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Payroll Management Controller
 * Base path: /api/payroll
 * All routes require SCHOOL_ADMIN or SUPER_ADMIN role
 */
@Slf4j
@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
@RequireAdmin
public class PayrollController {

    private final PayrollService payrollService;
    private final PdfGenerationService pdfGeneratorService;
    private final PayrollRepository payrollRepository;
    private final StaffProfileRepository staffProfileRepository;

    /**
     * Create or update salary structure for staff
     * POST /api/payroll/structure
     */
    @PostMapping("/structure")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> upsertSalaryStructure(
            @Valid @RequestBody UpsertSalaryStructureRequest request) {

        log.info("Upserting salary structure for staff ID: {}", request.getStaffId());

        SalaryStructureResponse salaryStructure = payrollService.upsertSalaryStructure(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Salary structure saved successfully", salaryStructure));
    }

    /**
     * Get salary structure for a staff member
     * GET /api/payroll/structure/:staffId
     */
    @GetMapping("/structure/{staffId}")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> getSalaryStructure(
            @PathVariable UUID staffId) {

        log.info("Fetching salary structure for staff ID: {}", staffId);

        SalaryStructureResponse salaryStructure = payrollService.getSalaryStructure(staffId);

        return ResponseEntity.ok(
                ApiResponse.success("Salary structure retrieved successfully", salaryStructure)
        );
    }

    /**
     * Generate payroll for a month
     * POST /api/payroll/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> generatePayroll(
            @Valid @RequestBody GeneratePayrollRequest request) {

        log.info("Generating payroll for month: {}, year: {}, staffIds: {}",
                request.getMonth(), request.getYear(), request.getStaffIds());

        List<PayrollResponse> payrolls = payrollService.generatePayroll(
                request.getMonth(),
                request.getYear(),
                request.getStaffIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithCount(
                        "Payroll generated successfully",
                        payrolls,
                        payrolls.size()
                ));
    }

    /**
     * Get all payrolls with optional filters
     * GET /api/payroll/list?month=JANUARY&year=2024
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> getPayrolls(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year) {

        log.info("Fetching payrolls with filters - month: {}, year: {}", month, year);

        List<PayrollResponse> payrolls = payrollService.getPayrolls(month, year);

        return ResponseEntity.ok(
                ApiResponse.successWithCount(
                        "Payrolls retrieved successfully",
                        payrolls,
                        payrolls.size()
                )
        );
    }

    /**
     * Get payslip PDF for a specific payroll
     * GET /api/payroll/payslip/:id
     */
    @GetMapping("/payslip/{id}")
    public ResponseEntity<ByteArrayResource> getPayslip(@PathVariable UUID id) {
        log.info("Generating payslip PDF for payroll ID: {}", id);

        // Get payroll
        PayrollResponse payroll = payrollService.getPayrollById(id);

        // Get staff profile
        StaffProfile staff = staffProfileRepository.findByIdWithUser(payroll.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        log.info("User loaded? --------------------------------> {}", Hibernate.isInitialized(staff.getUser()));
        // Generate PDF
        byte[] pdfBytes = pdfGeneratorService.generatePayslip(payroll, staff);

        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        String filename = String.format("Payslip_%s_%s_%d.pdf",
                staff.getEmployeeCode(),
                payroll.getMonth(),
                payroll.getYear());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @PutMapping("paid/{id}")
    public ResponseEntity<ApiResponse<PayrollResponse>> markAsPaid(@PathVariable UUID id) {
        PayrollResponse payroll = payrollService.markAsPaid(id);
        return ResponseEntity.ok(ApiResponse.success("Payroll updated successfully", payroll));
    }
}