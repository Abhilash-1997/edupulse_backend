package com.school.management.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for generating payroll
 * Used in POST /api/payroll/generate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePayrollRequest {

    @NotBlank(message = "Month is required")
    private String month; // Accepts "1" or "JANUARY"

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Invalid year")
    private Integer year;

    /**
     * Optional list of staff IDs
     * If null or empty, generates payroll for all active staff
     */
    private List<UUID> staffIds;
}