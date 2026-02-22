package com.school.management.dto.response;

import com.school.management.constant.FeeFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeStatusResponse {

    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private UUID classId;
    private String className;
    private String sectionName;
    private Float totalDue;
    private Float totalFeesPerStudent;
    private Float totalPaid;
    private Float balance;
    private String status; // PAID, PARTIAL, PENDING
    private List<FeeItem> fees;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeItem {
        private UUID feeStructureId;
        private String name;
        private Float amount;
        private FeeFrequency frequency;
        private String dueDate;
    }
}