package com.school.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBusLocationRequest {

    @NotNull(message = "Bus ID is required")
    private UUID busId;

    @NotNull(message = "Latitude is required")
    private BigDecimal lat;

    @NotNull(message = "Longitude is required")
    private BigDecimal lng;

    private Float speed;
    private Float heading;
    private Float accuracy;
}