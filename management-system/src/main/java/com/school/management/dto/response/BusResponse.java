package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusResponse {

    private UUID id;
    private String busNumber;
    private String registrationNumber;
    private Integer capacity;
    private Boolean isActive;
    private String deviceId;
    private UUID driverId;
    private UserResponse driver;
}