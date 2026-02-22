package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStatisticsResponse {

    private OverviewStats overview;
    private List<ClassStats> classSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewStats {
        private Float totalFees;
        private Float totalCollected;
        private Float totalPending;
        private Float overallCollectionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassStats {
        private String classId;
        private String className;
        private Integer studentCount;
        private Float totalFees;
        private Float collectedAmount;
        private Float pendingAmount;
        private Float collectionRate;
    }
}