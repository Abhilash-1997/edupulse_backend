package com.school.management.dto.response;

import com.school.management.constant.PayrollStatus;
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
public class PayrollResponse {

    private UUID id;
    private String month;
    private Integer year;
    private Float basicSalary;
    private List<Map<String, Object>> allowances;
    private Map<String, Object> deductionsBreakdown;
    private Map<String, Object> attendanceSummary;
    private Float bonus;
    private Float deductions;
    private Float netSalary;
    private PayrollStatus status;
    private LocalDate paymentDate;
    private UUID staffId;
    private StaffProfileResponse staff;
}