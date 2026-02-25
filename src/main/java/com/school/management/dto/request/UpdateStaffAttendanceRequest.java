package com.school.management.dto.request;

import com.school.management.constant.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Data
@Getter
@Setter
public class UpdateStaffAttendanceRequest {

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String remarks;
}
