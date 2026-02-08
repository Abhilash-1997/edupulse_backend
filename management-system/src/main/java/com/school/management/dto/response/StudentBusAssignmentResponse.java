package com.school.management.dto.response;

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
public class StudentBusAssignmentResponse {

    private UUID id;
    private String stopName;
    private LocalTime pickupTime;
    private LocalTime dropoffTime;
    private Boolean isActive;
    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private UUID busId;
    private String busNumber;
    private UUID routeId;
    private String routeName;
}