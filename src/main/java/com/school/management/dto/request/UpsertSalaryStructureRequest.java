package com.school.management.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertSalaryStructureRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;

    @NotNull(message = "Basic salary is required")
    @Min(value = 0, message = "Basic salary cannot be negative")
    private Float basicSalary;

    private List<Map<String, Object>> allowances;
    private List<Map<String, Object>> deductions;
}