package com.school.management.service;

import com.lowagie.text.DocumentException;
import com.school.management.dto.response.PayrollResponse;
import com.school.management.entity.FeePayment;
import com.school.management.entity.StaffProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    /**
     * Generates a PDF from a Thymeleaf template.
     *
     * @param templateName the name of the Thymeleaf template (without .html
     *                     extension)
     * @param variables    the variables to pass to the template
     * @return byte array containing the PDF
     */
    public byte[] generatePdf(String templateName, Map<String, Object> variables) {
        // Create Thymeleaf context with variables
        Context context = new Context();
        context.setVariables(variables);

        // Process the template to HTML
        String htmlContent = templateEngine.process(templateName, context);

        // Convert HTML to PDF using Flying Saucer
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Error generating PDF from template: {}", templateName, e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    public byte[] generatePayslip(PayrollResponse payroll, StaffProfile staff) {
        log.info("Generating payslip for staff: {} for {}/{}",
                staff.getEmployeeCode(), payroll.getMonth(), payroll.getYear());

        Map<String, Object> variables = new HashMap<>();

        // Staff Information
        variables.put("employeeName", staff.getUser().getName());
        variables.put("employeeCode", staff.getEmployeeCode());
        variables.put("designation", staff.getDesignation());
        variables.put("department", staff.getDepartment());
        variables.put("joiningDate", formatDate(staff.getJoiningDate()));

        // Payroll Period
        variables.put("month", payroll.getMonth());
        variables.put("year", payroll.getYear());
        variables.put("payrollMonth", payroll.getMonth() + " " + payroll.getYear());

        // Salary Components
        variables.put("basicSalary", formatCurrency(payroll.getBasicSalary()));
        variables.put("basicSalaryRaw", payroll.getBasicSalary());

        // Allowances
        Float totalAllowances = 0.0f;
        if (payroll.getAllowances() != null && !payroll.getAllowances().isEmpty()) {
            variables.put("allowances", payroll.getAllowances());
            totalAllowances = payroll.getAllowances().stream()
                    .map(allowance -> ((Number) allowance.get("amount")).floatValue())
                    .reduce(0.0f, Float::sum);
        }
        variables.put("totalAllowances", formatCurrency(totalAllowances));
        variables.put("totalAllowancesRaw", totalAllowances);

        // Gross Salary
        Float grossSalary = payroll.getBasicSalary() + totalAllowances;
        variables.put("grossSalary", formatCurrency(grossSalary));
        variables.put("grossSalaryRaw", grossSalary);

        // Bonus
        Float bonus = payroll.getBonus() != null ? payroll.getBonus() : 0.0f;
        variables.put("bonus", formatCurrency(bonus));
        variables.put("bonusRaw", bonus);

        // Deductions
        variables.put("totalDeductions", formatCurrency(payroll.getDeductions()));
        variables.put("totalDeductionsRaw", payroll.getDeductions());

        // Deductions Breakdown
        if (payroll.getDeductionsBreakdown() != null) {
            variables.put("deductionsBreakdown", payroll.getDeductionsBreakdown());

            // Extract specific deduction amounts
            Map<String, Object> breakdown = payroll.getDeductionsBreakdown();
            variables.put("fixedDeductions", formatCurrency(
                    ((Number) breakdown.getOrDefault("fixed", 0.0)).floatValue()));
            variables.put("lopAmount", formatCurrency(
                    ((Number) breakdown.getOrDefault("lopAmount", 0.0)).floatValue()));
            variables.put("proRataAmount", formatCurrency(
                    ((Number) breakdown.getOrDefault("proRataAmount", 0.0)).floatValue()));
        }

        // Net Salary
        variables.put("netSalary", formatCurrency(payroll.getNetSalary()));
        variables.put("netSalaryRaw", payroll.getNetSalary());
        variables.put("netSalaryInWords", convertToWords(payroll.getNetSalary()));

        // Attendance Summary
        if (payroll.getAttendanceSummary() != null) {
            variables.put("attendanceSummary", payroll.getAttendanceSummary());

            Map<String, Object> attendance = payroll.getAttendanceSummary();
            variables.put("totalDays", attendance.get("totalDays"));
            variables.put("effectiveDays", attendance.get("effectiveDays"));
            variables.put("presentDays", attendance.get("present"));
            variables.put("absentDays", attendance.get("absent"));
            variables.put("halfDays", attendance.get("halfDays"));
            variables.put("leaveDays", attendance.get("leaves"));
            variables.put("lopDays", attendance.get("lopDays"));
        }

        // Payment Information
        variables.put("status", payroll.getStatus().name());
        variables.put("paymentDate",
                payroll.getPaymentDate() != null ? formatDate(payroll.getPaymentDate()) : "Not Paid");
        variables.put("generatedDate", formatDate(LocalDate.now()));

        return generatePdf("pdf/payslip", variables);
    }

    /**
     * Format currency in Indian Rupee format
     */
    private String formatCurrency(Float amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return formatter.format(amount);
    }

    /**
     * Format date in dd-MMM-yyyy format
     */
    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        return date.format(formatter);
    }

    /**
     * Convert number to words (Indian Rupee format)
     */
    private String convertToWords(Float amount) {
        if (amount == null || amount == 0) {
            return "Zero Rupees Only";
        }

        long rupees = amount.longValue();
        long paise = Math.round((amount - rupees) * 100);

        String rupeesInWords = convertNumberToWords(rupees);

        if (paise > 0) {
            String paiseInWords = convertNumberToWords(paise);
            return rupeesInWords + " Rupees and " + paiseInWords + " Paise Only";
        } else {
            return rupeesInWords + " Rupees Only";
        }
    }

    /**
     * Convert number to words helper
     */
    private String convertNumberToWords(long number) {
        if (number == 0) {
            return "Zero";
        }

        String[] ones = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine" };
        String[] tens = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };
        String[] teens = { "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
                "Eighteen", "Nineteen" };

        if (number < 10) {
            return ones[(int) number];
        } else if (number < 20) {
            return teens[(int) (number - 10)];
        } else if (number < 100) {
            return tens[(int) (number / 10)] + (number % 10 != 0 ? " " + ones[(int) (number % 10)] : "");
        } else if (number < 1000) {
            return ones[(int) (number / 100)] + " Hundred"
                    + (number % 100 != 0 ? " " + convertNumberToWords(number % 100) : "");
        } else if (number < 100000) {
            return convertNumberToWords(number / 1000) + " Thousand"
                    + (number % 1000 != 0 ? " " + convertNumberToWords(number % 1000) : "");
        } else if (number < 10000000) {
            return convertNumberToWords(number / 100000) + " Lakh"
                    + (number % 100000 != 0 ? " " + convertNumberToWords(number % 100000) : "");
        } else {
            return convertNumberToWords(number / 10000000) + " Crore"
                    + (number % 10000000 != 0 ? " " + convertNumberToWords(number % 10000000) : "");
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateFeeReceipt(FeePayment feePayment) {
        log.info("Generating fee receipt for payment ID: {}", feePayment.getId());

        Map<String, Object> variables = new HashMap<>();

        // School Information
        variables.put("schoolName", feePayment.getSchool().getName());
        variables.put("schoolAddress", feePayment.getSchool().getAddress() != null
                ? feePayment.getSchool().getAddress()
                : "");

        // Student Information
        variables.put("studentName", feePayment.getStudent().getName());
        variables.put("admissionNumber", feePayment.getStudent().getAdmissionNumber());

        String className = "N/A";
        if (feePayment.getStudent().getClassEntity() != null) {
            className = feePayment.getStudent().getClassEntity().getName();
            if (feePayment.getStudent().getSection() != null) {
                className += " - " + feePayment.getStudent().getSection().getName();
            }
        }
        variables.put("className", className);

        // Guardian Information
        if (feePayment.getStudent().getParent() != null) {
            variables.put("guardianName", feePayment.getStudent().getParent().getGuardianName());
            if (feePayment.getStudent().getParent().getUser() != null) {
                variables.put("contact", feePayment.getStudent().getParent().getUser().getPhone());
            }
        } else {
            variables.put("guardianName", "N/A");
            variables.put("contact", "N/A");
        }

        // Fee Details
        variables.put("feeName", feePayment.getFeeStructure().getName());
        variables.put("feeAmount", formatCurrency(feePayment.getFeeStructure().getAmount()));
        variables.put("frequency", feePayment.getFeeStructure().getFrequency().name());

        // Payment Details
        variables.put("amountPaid", formatCurrency(feePayment.getAmountPaid()));
        variables.put("amountPaidInWords", convertToWords(feePayment.getAmountPaid()));
        variables.put("paymentDate", formatDate(feePayment.getPaymentDate()));
        variables.put("paymentMethod", feePayment.getPaymentMethod().name());
        variables.put("transactionId", feePayment.getTransactionId() != null
                ? feePayment.getTransactionId()
                : "N/A");
        variables.put("paymentStatus", feePayment.getStatus().name());

        // Receipt metadata
        variables.put("receiptNumber", "RCP-" + feePayment.getId().toString().substring(0, 8).toUpperCase());
        variables.put("generatedDate", formatDate(LocalDate.now()));

        return generatePdf("pdf/fee-receipt", variables);
    }
}
