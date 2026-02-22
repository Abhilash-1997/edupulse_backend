package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStructureResponse {

    private UUID id;
    private Float basicSalary;
    private List<Map<String, Object>> allowances;
    private List<Map<String, Object>> deductions;
    private Float netSalary;
    private LocalDate effectiveDate;
    private UUID staffId;
}