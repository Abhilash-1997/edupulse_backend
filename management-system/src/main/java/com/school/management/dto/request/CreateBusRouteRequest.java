package com.school.management.dto.request;

import com.school.management.constant.BusRouteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBusRouteRequest {

    @NotNull(message = "Bus ID is required")
    private UUID busId;

    @NotBlank(message = "Route name is required")
    private String routeName;

    private BusRouteType routeType;

    private List<Map<String, Object>> stops;
}