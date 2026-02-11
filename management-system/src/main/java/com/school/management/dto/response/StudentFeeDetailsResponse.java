package com.school.management.dto.response;

import com.school.management.constant.FeeFrequency;
import com.school.management.constant.PaymentMethod;
import com.school.management.constant.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeDetailsResponse {

    private StudentInfo student;
    private FeeSummary summary;
    private List<FeeBreakdownItem> feeBreakdown;
    private List<PaymentHistoryItem> paymentHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private UUID id;
        private String name;
        private String admissionNumber;
        private String className;
        private String guardianName;
        private String contact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeSummary {
        private Float totalFees;
        private Float totalPaid;
        private Float totalPending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeBreakdownItem {
        private UUID feeStructureId;
        private String feeName;
        private Float amount;
        private FeeFrequency frequency;
        private Float totalPaid;
        private Float pendingAmount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentHistoryItem {
        private UUID id;
        private String feeName;
        private Float amountPaid;
        private LocalDate paymentDate;
        private PaymentMethod paymentMethod;
        private String transactionId;
        private PaymentStatus status;
    }
}
