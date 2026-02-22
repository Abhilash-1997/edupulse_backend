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
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAttendanceRequest {

    @NotNull(message = "Section ID is required")
    private UUID sectionId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotEmpty(message = "Students list cannot be empty")
    @Valid
    private List<StudentAttendance> students;

    private List<StudentAttendance> attendance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAttendance {

        @NotNull(message = "Student ID is required")
        private UUID studentId;

        @NotNull(message = "Status is required")
        private AttendanceStatus status;
    }
}