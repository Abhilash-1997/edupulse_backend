package com.school.management.controller;

import com.school.management.constant.BusTripStatus;
import com.school.management.dto.request.*;
import com.school.management.dto.response.*;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.service.TransportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transport Management Controller
 * Base path: /transport
 */
@Slf4j
@RestController
@RequestMapping("/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    // ======================== BUS MANAGEMENT ========================

    /**
     * Create a new bus (Admin only)
     * POST /transport/buses
     */
    @PostMapping("/buses")
    @RequireAdmin
    public ResponseEntity<ApiResponse<BusResponse>> createBus(
            @Valid @RequestBody CreateBusRequest request) {
        log.info("Creating bus: {}", request.getBusNumber());
        BusResponse response = transportService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bus created successfully", response));
    }

    /**
     * Get all buses (Admin, Staff, Bus Driver, Parent)
     * GET /transport/buses?isActive=true
     */
    @GetMapping("/buses")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER', 'PARENT')")
    public ResponseEntity<ApiResponse<List<BusResponse>>> getAllBuses(
            @RequestParam(required = false) Boolean isActive) {
        log.info("Fetching all buses, isActive: {}", isActive);
        List<BusResponse> response = transportService.getAllBuses(isActive);
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Buses retrieved successfully", response, response.size()));
    }

    /**
     * Get bus by ID (Admin, Staff, Bus Driver)
     * GET /transport/buses/{id}
     */
    @GetMapping("/buses/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<BusResponse>> getBusById(@PathVariable UUID id) {
        log.info("Fetching bus by ID: {}", id);
        BusResponse response = transportService.getBusById(id);
        return ResponseEntity.ok(ApiResponse.success("Bus retrieved successfully", response));
    }

    /**
     * Update bus (Admin only)
     * PATCH /transport/buses/{id}
     */
    @PatchMapping("/buses/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<BusResponse>> updateBus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBusRequest request) {
        log.info("Updating bus: {}", id);
        BusResponse response = transportService.updateBus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Bus updated successfully", response));
    }

    /**
     * Delete bus (Admin only)
     * DELETE /transport/buses/{id}
     */
    @DeleteMapping("/buses/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> deleteBus(@PathVariable UUID id) {
        log.info("Deleting bus: {}", id);
        transportService.deleteBus(id);
        return ResponseEntity.ok(ApiResponse.success("Bus deleted successfully", null));
    }

    // ======================== ROUTE MANAGEMENT ========================

    /**
     * Create a new route (Admin only)
     * POST /transport/routes
     */
    @PostMapping("/routes")
    @RequireAdmin
    public ResponseEntity<ApiResponse<BusRouteResponse>> createRoute(
            @Valid @RequestBody CreateBusRouteRequest request) {
        log.info("Creating route: {}", request.getRouteName());
        BusRouteResponse response = transportService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Route created successfully", response));
    }

    /**
     * Get all routes (Admin, Staff, Bus Driver)
     * GET /transport/routes?busId=...
     */
    @GetMapping("/routes")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<List<BusRouteResponse>>> getRoutes(
            @RequestParam(required = false) UUID busId) {
        log.info("Fetching routes, busId: {}", busId);
        List<BusRouteResponse> response = transportService.getRoutes(busId);
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Routes retrieved successfully", response, response.size()));
    }

    /**
     * Update route (Admin only)
     * PATCH /transport/routes/{id}
     */
    @PatchMapping("/routes/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<BusRouteResponse>> updateRoute(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBusRouteRequest request) {
        log.info("Updating route: {}", id);
        BusRouteResponse response = transportService.updateRoute(id, request);
        return ResponseEntity.ok(ApiResponse.success("Route updated successfully", response));
    }

    /**
     * Delete route (Admin only)
     * DELETE /transport/routes/{id}
     */
    @DeleteMapping("/routes/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable UUID id) {
        log.info("Deleting route: {}", id);
        transportService.deleteRoute(id);
        return ResponseEntity.ok(ApiResponse.success("Route deleted successfully", null));
    }

    // ======================== TRIP MANAGEMENT ========================

    /**
     * Start a trip (Admin, Staff, Bus Driver)
     * POST /transport/trips/{busId}/start
     */
    @PostMapping("/trips/{busId}/start")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<BusTripResponse>> startTrip(
            @PathVariable UUID busId,
            @Valid @RequestBody StartTripRequest request) {
        log.info("Starting trip for bus: {}", busId);
        BusTripResponse response = transportService.startTrip(busId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip started successfully", response));
    }

    /**
     * End a trip (Admin, Staff, Bus Driver)
     * POST /transport/trips/{busId}/end
     */
    @PostMapping("/trips/{busId}/end")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<BusTripResponse>> endTrip(
            @PathVariable UUID busId,
            @RequestBody(required = false) EndTripRequest request) {
        log.info("Ending trip for bus: {}", busId);
        BusTripResponse response = transportService.endTrip(busId, request != null ? request : new EndTripRequest());
        return ResponseEntity.ok(ApiResponse.success("Trip ended successfully", response));
    }

    /**
     * Get trip history (Admin, Staff, Bus Driver)
     * GET /transport/trips/history?busId=...&status=...&startDate=...&endDate=...
     */
    @GetMapping("/trips/history")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<List<BusTripResponse>>> getTripHistory(
            @RequestParam(required = false) UUID busId,
            @RequestParam(required = false) BusTripStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching trip history, busId: {}, status: {}", busId, status);
        List<BusTripResponse> response = transportService.getTrips(busId, status, startDate, endDate);
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Trip history retrieved successfully", response, response.size()));
    }

    // ======================== LOCATION TRACKING ========================

    /**
     * Update bus location (Admin, Staff, Bus Driver)
     * POST /transport/location
     */
    @PostMapping("/location")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<BusLocationResponse>> updateLocation(
            @Valid @RequestBody UpdateBusLocationRequest request) {
        log.info("Updating location for bus: {}", request.getBusId());
        BusLocationResponse response = transportService.updateLocation(request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", response));
    }

    /**
     * Get live bus location (Admin, Staff, Parent, Bus Driver)
     * GET /transport/location/live?busId=...
     */
    @GetMapping("/location/live")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'PARENT', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<LiveBusLocationResponse>> getLiveLocation(
            @RequestParam UUID busId) {
        log.info("Fetching live location for bus: {}", busId);
        LiveBusLocationResponse response = transportService.getLiveLocation(busId);
        return ResponseEntity.ok(ApiResponse.success("Live location retrieved successfully", response));
    }

    /**
     * Get location history for a trip (Admin, Staff, Bus Driver)
     * GET /transport/location/history/{tripId}
     */
    @GetMapping("/location/history/{tripId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<List<BusLocationResponse>>> getLocationHistory(
            @PathVariable UUID tripId) {
        log.info("Fetching location history for trip: {}", tripId);
        List<BusLocationResponse> response = transportService.getLocationHistory(tripId);
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Location history retrieved successfully", response, response.size()));
    }

    // ======================== STUDENT ASSIGNMENTS ========================

    /**
     * Assign student to bus (Admin, Staff)
     * POST /transport/assignments
     */
    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<StudentBusAssignmentResponse>> assignStudentToBus(
            @Valid @RequestBody AssignStudentToBusRequest request) {
        log.info("Assigning student {} to bus {}", request.getStudentId(), request.getBusId());
        StudentBusAssignmentResponse response = transportService.assignStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student assigned to bus successfully", response));
    }

    /**
     * Get bus assignments (Admin, Staff, Bus Driver)
     * GET /transport/assignments?busId=...
     */
    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'STAFF', 'BUS_DRIVER')")
    public ResponseEntity<ApiResponse<List<StudentBusAssignmentResponse>>> getBusAssignments(
            @RequestParam(required = false) UUID busId) {
        log.info("Fetching bus assignments, busId: {}", busId);
        List<StudentBusAssignmentResponse> response = transportService.getAssignments(busId);
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Assignments retrieved successfully", response, response.size()));
    }

    /**
     * Remove student from bus (Admin only)
     * DELETE /transport/assignments/{studentId}
     */
    @DeleteMapping("/assignments/{studentId}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> removeStudentFromBus(@PathVariable UUID studentId) {
        log.info("Removing student {} from bus", studentId);
        transportService.unassignStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success("Student removed from bus successfully", null));
    }
}
