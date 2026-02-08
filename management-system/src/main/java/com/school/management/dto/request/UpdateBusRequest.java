package com.school.management.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBusRequest {

    private String busNumber;
    private String registrationNumber;
    private UUID driverId;
    private String deviceId;
    private Integer capacity;
    private Boolean isActive;
}