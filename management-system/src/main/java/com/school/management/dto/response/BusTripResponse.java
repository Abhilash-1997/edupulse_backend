package com.school.management.dto.response;

import com.school.management.constant.BusTripStatus;
import com.school.management.constant.BusTripType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusTripResponse {

    private UUID id;
    private BusTripType tripType;
    private BusTripStatus status;
    private Integer lastReachedStopOrder;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private UUID busId;
    private UUID routeId;
    private BusResponse bus;
    private BusRouteResponse route;
}