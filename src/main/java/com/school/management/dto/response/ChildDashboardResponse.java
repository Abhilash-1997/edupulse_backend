package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDashboardResponse {

    private List<TimetableResponse> timetable;
    private List<AttendanceResponse> recentAttendance;
    private Map<String, List<ExamResultResponse>> examResults; // Grouped by examId
    private FeeInfo feeInfo;
    private Float attendancePercentage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeInfo {
        private Float totalFees;
        private Float paidAmount;
        private Float pendingAmount;
        private List<FeePaymentResponse> recentPayments;
    }
}