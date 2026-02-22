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
public class MyChildrenResponse {

    private List<ChildInfoResponse> children;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildInfoResponse {
        private StudentResponse student;
        private FeeSummary feeSummary;
        private Float attendanceRate;
        private Float latestExamAverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeSummary {
        private Float totalFees;
        private Float paidFees;
        private Float pendingFees;
    }
}