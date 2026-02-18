package com.school.management.service;

import com.school.management.constant.AttendanceStatus;
import com.school.management.constant.PayrollStatus;
import com.school.management.constant.StaffStatus;
import com.school.management.dto.request.CreatePayrollRequest;
import com.school.management.dto.request.UpsertSalaryStructureRequest;
import com.school.management.dto.response.PayrollResponse;
import com.school.management.dto.response.SalaryStructureResponse;
import com.school.management.dto.response.StaffProfileResponse;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;
    private final SchoolRepository schoolRepository;
    private final PdfGenerationService pdfGeneratorService;
    private final EmailService emailService;

    /**
     * Generate payroll for a specific month and year
     * Can generate for specific staff IDs or all active staff
     */
    @Transactional
    public List<PayrollResponse> generatePayroll(String monthInput, Integer year, List<UUID> staffIds) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Parse month (accepts 1-12 or month name like "JANUARY")
        String monthName;
        int monthNumber;

        try {
            monthNumber = Integer.parseInt(monthInput);
            if (monthNumber < 1 || monthNumber > 12) {
                throw new BadRequestException("Invalid month number. Must be between 1 and 12");
            }
            monthName = Month.of(monthNumber).name();
        } catch (NumberFormatException e) {
            // Try parsing as month name
            try {
                Month month = Month.valueOf(monthInput.toUpperCase());
                monthNumber = month.getValue();
                monthName = month.name();
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid month: " + monthInput +
                        ". Use month number (1-12) or month name (e.g., JANUARY)");
            }
        }

        // Get days in month
        YearMonth yearMonth = YearMonth.of(year, monthNumber);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate lastDayOfMonth = LocalDate.of(year, monthNumber, daysInMonth);

        // Get staff list
        List<StaffProfile> staffList;
        if (staffIds != null && !staffIds.isEmpty()) {
            // Generate for specific staff
            staffList = staffProfileRepository.findActiveStaffBySchoolAndIds(schoolId, staffIds);
        } else {
            // Generate for all active staff who joined before end of month
            staffList = staffProfileRepository.findActiveStaffBySchoolAndJoinedBeforeDate(
                    schoolId, lastDayOfMonth);
        }

        if (staffList.isEmpty()) {
            throw new BadRequestException("No eligible staff found for payroll generation");
        }

        List<PayrollResponse> responses = new ArrayList<>();

        for (StaffProfile staff : staffList) {
            try {
                // Get salary structure
                SalaryStructure salaryStructure = salaryStructureRepository
                        .findByStaff_IdAndDeletedAtIsNull(staff.getId())
                        .orElse(null);

                if (salaryStructure == null) {
                    log.warn("No salary structure for staff: {} ({}). Skipping payroll generation.",
                            staff.getUser().getName(), staff.getEmployeeCode());
                    continue;
                }

                // Calculate payroll
                PayrollResponse payroll = calculatePayrollForStaff(
                        school, staff, salaryStructure, monthName, year, daysInMonth);

                responses.add(payroll);

                log.info("User loaded? {}", Hibernate.isInitialized(staff.getUser()));
                // Generate and send payslip (async - errors caught silently)
                try {
                    byte[] pdfBytes = pdfGeneratorService.generatePayslip(payroll, staff);
                    emailService.sendPayslipEmail(
                            staff.getUser().getEmail(),
                            monthName,
                            year,
                            pdfBytes,
                            school.getName()
                    );
                } catch (Exception e) {
                    log.error("Failed to send payslip for staff: {} ({})",
                            staff.getUser().getName(), staff.getEmployeeCode(), e);
                }

            } catch (Exception e) {
                log.error("Error generating payroll for staff: {} ({})",
                        staff.getUser().getName(), staff.getEmployeeCode(), e);
            }
        }

        return responses;
    }

    /**
     * Calculate payroll for a single staff member
     * Complex LOP calculation logic
     */
    private PayrollResponse calculatePayrollForStaff(
            School school,
            StaffProfile staff,
            SalaryStructure salaryStructure,
            String month,
            Integer year,
            int daysInMonth) {

        // 1. Calculate gross salary (Basic + Allowances)
        Float basicSalary = salaryStructure.getBasicSalary();

        Float allowancesTotal = 0.0f;
        if (salaryStructure.getAllowances() != null) {
            allowancesTotal = salaryStructure.getAllowances().stream()
                    .map(allowance -> {
                        Object amountObj = allowance.get("amount");
                        if (amountObj instanceof Number) {
                            return ((Number) amountObj).floatValue();
                        }
                        return 0.0f;
                    })
                    .reduce(0.0f, Float::sum);
        }

        Float grossSalary = basicSalary + allowancesTotal;
        Float perDayPay = grossSalary / daysInMonth;

        // 2. Pro-rata calculation for mid-month joining
        LocalDate joiningDate = staff.getJoiningDate();
        int effectiveEmployedDays = daysInMonth;
        Float proRataDeduction = 0.0f;

        Month monthEnum = Month.valueOf(month);
        if (joiningDate.getYear() == year && joiningDate.getMonth() == monthEnum) {
            int daysNotWorked = joiningDate.getDayOfMonth() - 1;
            effectiveEmployedDays = daysInMonth - daysNotWorked;
            proRataDeduction = daysNotWorked * perDayPay;

            log.info("Pro-rata applied for staff {} - Joined on {}, Days not worked: {}, Deduction: {}",
                    staff.getEmployeeCode(), joiningDate, daysNotWorked, proRataDeduction);
        }

        // 3. Get attendance for the month
        LocalDate startDate = LocalDate.of(year, monthEnum, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<StaffAttendance> attendances = staffAttendanceRepository
                .findByStaffIdAndDateRange(staff.getId(), startDate, endDate);

        // 4. Count attendance statuses
        long presentCount = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .count();

        long halfDayCount = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY)
                .count();

        long absentCount = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .count();

        // Leave count = days with no attendance record (excluding absents and recorded days)
        long recordedDays = presentCount + halfDayCount + absentCount;
        long leaveCount = effectiveEmployedDays - recordedDays;

        // 5. LOP Calculation (Loss of Pay)
        // Rule: 2 paid leaves allowed, 2 half days allowed
        long deductibleLeaves = Math.max(0, leaveCount - 2);
        long deductibleHalfDays = Math.max(0, halfDayCount - 2);

        // Total LOP days = all absents + excess leaves + (excess half days * 0.5)
        Float totalLOPDays = (float) absentCount + deductibleLeaves + (deductibleHalfDays * 0.5f);

        log.info("LOP calculation for staff {}: Present={}, HalfDay={}, Absent={}, Leave={}, " +
                        "Deductible Leaves={}, Deductible HalfDays={}, Total LOP Days={}",
                staff.getEmployeeCode(), presentCount, halfDayCount, absentCount, leaveCount,
                deductibleLeaves, deductibleHalfDays, totalLOPDays);

        // 6. Calculate deductions
        Float fixedDeductions = 0.0f;
        if (salaryStructure.getDeductions() != null) {
            fixedDeductions = salaryStructure.getDeductions().stream()
                    .map(deduction -> {
                        Object amountObj = deduction.get("amount");
                        if (amountObj instanceof Number) {
                            return ((Number) amountObj).floatValue();
                        }
                        return 0.0f;
                    })
                    .reduce(0.0f, Float::sum);
        }

        Float lopAmount = totalLOPDays * perDayPay;
        Float totalDeductions = fixedDeductions + lopAmount + proRataDeduction;

        // 7. Calculate final net salary
        Float finalNetSalary = Math.max(0, grossSalary - totalDeductions);
        finalNetSalary = Math.round(finalNetSalary * 100) / 100.0f; // Round to 2 decimals

        // 8. Build attendance summary (for payslip display)
        Map<String, Object> attendanceSummary = new HashMap<>();
        attendanceSummary.put("totalDays", daysInMonth);
        attendanceSummary.put("effectiveDays", effectiveEmployedDays);
        attendanceSummary.put("present", presentCount);
        attendanceSummary.put("absent", absentCount);
        attendanceSummary.put("halfDays", halfDayCount);
        attendanceSummary.put("leaves", leaveCount);
        attendanceSummary.put("lopDays", totalLOPDays);

        // 9. Build deductions breakdown (for transparency)
        Map<String, Object> deductionsBreakdown = new HashMap<>();
        deductionsBreakdown.put("fixed", fixedDeductions);
        deductionsBreakdown.put("lopAmount", lopAmount);
        deductionsBreakdown.put("proRataAmount", proRataDeduction);

        Map<String, Object> details = new HashMap<>();
        details.put("perDayPay", perDayPay);
        details.put("deductibleLeaves", deductibleLeaves);
        details.put("deductibleHalfDays", deductibleHalfDays);
        deductionsBreakdown.put("details", details);

        // 10. Upsert payroll record
        Payroll payroll = payrollRepository
                .findBySchool_IdAndStaff_IdAndMonthAndYearAndDeletedAtIsNull(
                        school.getId(), staff.getId(), month, year)
                .orElse(Payroll.builder()
                        .school(school)
                        .staff(staff)
                        .month(month)
                        .year(year)
                        .build());

        payroll.setBasicSalary(basicSalary);
        payroll.setAllowances(salaryStructure.getAllowances());
        payroll.setBonus(0.0f); // Default bonus is 0
        payroll.setDeductions(totalDeductions);
        payroll.setNetSalary(finalNetSalary);
        payroll.setDeductionsBreakdown(deductionsBreakdown);
        payroll.setAttendanceSummary(attendanceSummary);
        payroll.setStatus(PayrollStatus.GENERATED);

        payroll = payrollRepository.save(payroll);

        log.info("Payroll generated for staff {} - Month: {}, Year: {}, Net Salary: {}",
                staff.getEmployeeCode(), month, year, finalNetSalary);

        return mapToPayrollResponse(payroll);
    }

    /**
     * Get all payroll records with optional filters
     */
    @Transactional(readOnly = true)
    public List<PayrollResponse> getPayrolls(String month, Integer year) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        String normalizedMonth = normalizeMonth(month);

        List<Payroll> payrolls;
        if (normalizedMonth != null && year != null) {
            payrolls = payrollRepository.findBySchool_IdAndMonthAndYearAndDeletedAtIsNull(
                            schoolId, normalizedMonth, year);

        } else if (normalizedMonth != null) {
            payrolls = payrollRepository.findBySchool_IdAndMonthAndDeletedAtIsNull(
                            schoolId, normalizedMonth);

        } else if (year != null) {
            payrolls = payrollRepository.findBySchool_IdAndYearAndDeletedAtIsNull(
                            schoolId, year);

        } else {
            payrolls = payrollRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        }

        return payrolls.stream()
                .map(this::mapToPayrollResponse)
                .collect(Collectors.toList());

    }

    /**
     * Get payroll by ID
     */
    @Transactional(readOnly = true)
    public PayrollResponse getPayrollById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Payroll payroll = payrollRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));

        if (!payroll.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Payroll not found");
        }

        return mapToPayrollResponse(payroll);
    }

    /**
     * Get payroll for a specific staff member
     */
    @Transactional(readOnly = true)
    public List<PayrollResponse> getStaffPayrolls(UUID staffId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Payroll> payrolls = payrollRepository.findBySchool_IdAndStaff_IdAndDeletedAtIsNull(
                schoolId, staffId);

        return payrolls.stream()
                .map(this::mapToPayrollResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark payroll as paid
     */
    @Transactional
    public PayrollResponse markAsPaid(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Payroll payroll = payrollRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));

        if (!payroll.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Payroll not found");
        }

        if (payroll.getStatus() == PayrollStatus.PAID) {
            throw new BadRequestException("Payroll already marked as paid");
        }

        payroll.setStatus(PayrollStatus.PAID);
        payroll.setPaymentDate(LocalDate.now());

        payroll = payrollRepository.save(payroll);

        return mapToPayrollResponse(payroll);
    }

    /**
     * Delete payroll record
     */
    @Transactional
    public void deletePayroll(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Payroll payroll = payrollRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));

        if (!payroll.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Payroll not found");
        }

        if (payroll.getStatus() == PayrollStatus.PAID) {
            throw new BadRequestException("Cannot delete paid payroll");
        }

        payrollRepository.delete(payroll);
    }

    /**
     * Update payroll manually (for corrections)
     */
    @Transactional
    public PayrollResponse updatePayroll(UUID id, CreatePayrollRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Payroll payroll = payrollRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found"));

        if (!payroll.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Payroll not found");
        }

        if (payroll.getStatus() == PayrollStatus.PAID) {
            throw new BadRequestException("Cannot update paid payroll");
        }

        // Update fields if provided
        if (request.getBasicSalary() != null) {
            payroll.setBasicSalary(request.getBasicSalary());
        }
        if (request.getBonus() != null) {
            payroll.setBonus(request.getBonus());
        }
        if (request.getDeductions() != null) {
            payroll.setDeductions(request.getDeductions());
        }

        // Recalculate net salary
        Float basicSalary = payroll.getBasicSalary();
        Float allowancesTotal = payroll.getAllowances() != null ?
                payroll.getAllowances().stream()
                        .map(a -> ((Number) a.get("amount")).floatValue())
                        .reduce(0.0f, Float::sum) : 0.0f;
        Float bonus = payroll.getBonus() != null ? payroll.getBonus() : 0.0f;
        Float deductions = payroll.getDeductions() != null ? payroll.getDeductions() : 0.0f;

        Float netSalary = basicSalary + allowancesTotal + bonus - deductions;
        netSalary = Math.round(netSalary * 100) / 100.0f;

        payroll.setNetSalary(netSalary);

        payroll = payrollRepository.save(payroll);

        return mapToPayrollResponse(payroll);
    }

    // ============= SALARY STRUCTURE MANAGEMENT =============

    /**
     * Create or update salary structure for staff
     */
    @Transactional
    public SalaryStructureResponse upsertSalaryStructure(UpsertSalaryStructureRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        StaffProfile staff = staffProfileRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                        request.getStaffId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        // Calculate net salary
        Float basicSalary = request.getBasicSalary();

        Float allowancesTotal = 0.0f;
        if (request.getAllowances() != null) {
            allowancesTotal = request.getAllowances().stream()
                    .map(a -> ((Number) a.get("amount")).floatValue())
                    .reduce(0.0f, Float::sum);
        }

        Float deductionsTotal = 0.0f;
        if (request.getDeductions() != null) {
            deductionsTotal = request.getDeductions().stream()
                    .map(d -> ((Number) d.get("amount")).floatValue())
                    .reduce(0.0f, Float::sum);
        }

        Float netSalary = basicSalary + allowancesTotal - deductionsTotal;
        netSalary = Math.round(netSalary * 100) / 100.0f;

        // Find or create salary structure
        SalaryStructure salaryStructure = salaryStructureRepository
                .findByStaff_IdAndDeletedAtIsNull(staff.getId())
                .orElse(SalaryStructure.builder()
                        .school(school)
                        .staff(staff)
                        .effectiveDate(LocalDate.now())
                        .build());

        salaryStructure.setBasicSalary(basicSalary);
        salaryStructure.setAllowances(request.getAllowances());
        salaryStructure.setDeductions(request.getDeductions());
        salaryStructure.setNetSalary(netSalary);

        salaryStructure = salaryStructureRepository.save(salaryStructure);

        return mapToSalaryStructureResponse(salaryStructure);
    }

    /**
     * Get salary structure for staff
     */
    @Transactional(readOnly = true)
    public SalaryStructureResponse getSalaryStructure(UUID staffId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        // Verify staff belongs to school
        staffProfileRepository.findByIdAndSchool_IdAndDeletedAtIsNull(staffId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        SalaryStructure salaryStructure = salaryStructureRepository
                .findByStaff_IdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));

        return mapToSalaryStructureResponse(salaryStructure);
    }

    /**
     * Delete salary structure
     */
    @Transactional
    public void deleteSalaryStructure(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        SalaryStructure salaryStructure = salaryStructureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));

        if (!salaryStructure.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Salary structure not found");
        }

        salaryStructureRepository.delete(salaryStructure);
    }

    // ============= MAPPERS =============

    private PayrollResponse mapToPayrollResponse(Payroll payroll) {
        return PayrollResponse.builder()
                .id(payroll.getId())
                .month(payroll.getMonth())
                .year(payroll.getYear())
                .basicSalary(payroll.getBasicSalary())
                .allowances(payroll.getAllowances())
                .deductionsBreakdown(payroll.getDeductionsBreakdown())
                .attendanceSummary(payroll.getAttendanceSummary())
                .bonus(payroll.getBonus())
                .deductions(payroll.getDeductions())
                .netSalary(payroll.getNetSalary())
                .status(payroll.getStatus())
                .paymentDate(payroll.getPaymentDate())
                .staffId(payroll.getStaff().getId())
                .staff(mapToStaffProfileResponse(payroll.getStaff()))
                .build();
    }

    private SalaryStructureResponse mapToSalaryStructureResponse(SalaryStructure salaryStructure) {
        return SalaryStructureResponse.builder()
                .id(salaryStructure.getId())
                .basicSalary(salaryStructure.getBasicSalary())
                .allowances(salaryStructure.getAllowances())
                .deductions(salaryStructure.getDeductions())
                .netSalary(salaryStructure.getNetSalary())
                .effectiveDate(salaryStructure.getEffectiveDate())
                .staffId(salaryStructure.getStaff().getId())
                .build();
    }

    private StaffProfileResponse mapToStaffProfileResponse(StaffProfile staff) {
        return StaffProfileResponse.builder()
                .id(staff.getId())
                .employeeCode(staff.getEmployeeCode())
                .department(staff.getDepartment())
                .designation(staff.getDesignation())
                .joiningDate(staff.getJoiningDate())
                .workingAs(staff.getWorkingAs())
                .status(staff.getStatus())
                .build();
    }

    private String normalizeMonth(String monthInput) {
        if (monthInput == null || monthInput.isBlank()) {
            return null;
        }
        monthInput = monthInput.trim();
        // If numeric (1-12)
        if (monthInput.matches("\\d+")) {
            int monthNumber = Integer.parseInt(monthInput);
            if (monthNumber < 1 || monthNumber > 12) {
                throw new IllegalArgumentException("Invalid month number: " + monthInput);
            }
            return java.time.Month.of(monthNumber).name();
        }
        return monthInput.toUpperCase();
    }

}