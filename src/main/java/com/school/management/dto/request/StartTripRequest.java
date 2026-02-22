package com.school.management.dto.request;

import com.school.management.constant.BusTripType;
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
public class StartTripRequest {

    private UUID routeId;
    private BusTripType tripType;
}