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
public class StaffAttendanceByDateResponse {

    private List<StaffAttendanceResponse> marked;
    private List<PendingStaffResponse> pending;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingStaffResponse {
        private java.util.UUID id;
        private String name;
        private String employeeCode;
        private String designation;
        private String department;
        private String profilePicture;
    }
}
