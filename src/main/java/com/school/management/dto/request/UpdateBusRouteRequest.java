package com.school.management.dto.request;

import com.school.management.constant.BusRouteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBusRouteRequest {

    private String routeName;
    private BusRouteType routeType;
    private List<Map<String, Object>> stops;
    private Boolean isActive;
}