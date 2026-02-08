package com.school.management.dto.request;

import com.school.management.constant.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkStaffAttendanceRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotEmpty(message = "Attendance data cannot be empty")
    @Valid
    private List<StaffAttendanceData> attendanceData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffAttendanceData {

        @NotNull(message = "Staff ID is required")
        private UUID staffId;

        @NotNull(message = "Status is required")
        private AttendanceStatus status;

        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private String remarks;
    }
}