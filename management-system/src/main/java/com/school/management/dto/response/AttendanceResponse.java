package com.school.management.dto.response;

import com.school.management.constant.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private UUID id;
    private LocalDate date;
    private AttendanceStatus status;
    private UUID studentId;
    private String studentName;
    private UUID recordedBy;
    private Boolean isLocked;
}