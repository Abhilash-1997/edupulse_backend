package com.school.management.dto.response;

import com.school.management.constant.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAttendanceResponse {

    private UUID id;
    private LocalDate date;
    private AttendanceStatus status;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String remarks;
    private UUID staffId;
    private StaffProfileResponse staff;
}