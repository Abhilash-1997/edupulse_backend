package com.school.management.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePayrollRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotBlank(message = "Month is required")
    private String month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Invalid year")
    private Integer year;

    @NotNull(message = "Basic salary is required")
    @Min(value = 0, message = "Basic salary cannot be negative")
    private Float basicSalary;

    private Float bonus;
    private Float deductions;
}