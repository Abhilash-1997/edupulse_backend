package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveBusLocationResponse {

    private BusResponse bus;
    private BusLocationResponse location;
    private BusTripResponse activeTrip;
}