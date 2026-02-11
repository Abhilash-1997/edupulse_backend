package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeStatusResponse {

    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private Float totalDue;
    private Float totalPaid;
    private Float balance;
    private String status; // PAID, PARTIAL, PENDING
}