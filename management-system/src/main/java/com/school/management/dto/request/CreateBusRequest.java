package com.school.management.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBusRequest {

    @NotBlank(message = "Bus number is required")
    private String busNumber;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    private UUID driverId;
    private String deviceId;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}