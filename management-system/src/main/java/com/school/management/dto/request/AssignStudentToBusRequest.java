package com.school.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignStudentToBusRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Bus ID is required")
    private UUID busId;

    private UUID routeId;
    private String stopName;
    private LocalTime pickupTime;
    private LocalTime dropoffTime;
}