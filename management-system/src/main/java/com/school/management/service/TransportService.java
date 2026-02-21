package com.school.management.service;

import com.school.management.constant.BusTripStatus;
import com.school.management.dto.request.*;
import com.school.management.dto.response.*;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.exception.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportService {

    private final BusRepository busRepository;
    private final BusRouteRepository busRouteRepository;
    private final BusTripRepository busTripRepository;
    private final BusLocationRepository busLocationRepository;
    private final StudentBusAssignmentRepository studentBusAssignmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StaffProfileRepository staffProfileRepository;

    // ============= BUS MANAGEMENT =============

    @Transactional
    public BusResponse createBus(CreateBusRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Check duplicate bus number
        if (busRepository.findBySchool_IdAndBusNumberAndDeletedAtIsNull(schoolId, request.getBusNumber()).isPresent()) {
            throw new BadRequestException("Bus number already exists");
        }

        // Check duplicate registration number
        if (busRepository.findBySchool_IdAndRegistrationNumberAndDeletedAtIsNull(
                schoolId, request.getRegistrationNumber()).isPresent()) {
            throw new BadRequestException("Registration number already exists");
        }

        Bus bus = Bus.builder()
                .school(school)
                .busNumber(request.getBusNumber())
                .registrationNumber(request.getRegistrationNumber())
                .deviceId(request.getDeviceId())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 40)
                .isActive(true)
                .build();

        if (request.getDriverId() != null) {
            User driver = userRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                    request.getDriverId(), schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
            bus.setDriver(driver);
        }

        bus = busRepository.save(bus);

        return mapToBusResponse(bus);
    }

    @Transactional(readOnly = true)
    public List<BusResponse> getAllBuses(Boolean isActive) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Bus> buses;
        if (isActive != null) {
            buses = busRepository.findBySchool_IdAndIsActiveAndDeletedAtIsNull(schoolId, isActive);
        } else {
            buses = busRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        }

        return buses.stream()
                .map(this::mapToBusResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BusResponse getBusById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Bus bus = busRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        return mapToBusResponse(bus);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('BUS_DRIVER')")
    public BusResponse getDriverBus() {
        UUID driverId = SecurityUtils.getCurrentUserId();
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Bus bus = busRepository.findByDriver_IdAndSchool_IdAndDeletedAtIsNull(driverId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("No bus assigned to this driver"));

        // Fetch active routes for this bus
        List<BusRoute> activeRoutes = busRouteRepository.findByBus_IdAndIsActiveAndDeletedAtIsNull(
                bus.getId(), true);

        List<BusRouteResponse> routeResponses = activeRoutes.stream()
                .map(this::mapToBusRouteResponse)
                .collect(Collectors.toList());

        BusResponse response = mapToBusResponse(bus);
        response.setRoutes(routeResponses);

        return response;
    }

    @Transactional
    public BusResponse updateBus(UUID id, UpdateBusRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Bus bus = busRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        if (busRepository.findBySchool_IdAndBusNumberAndDeletedAtIsNull(schoolId, request.getBusNumber()).isPresent()) {
            throw new BadRequestException("Bus number already exists");
        }

        if (busRepository.findBySchool_IdAndRegistrationNumberAndDeletedAtIsNull(
                schoolId, request.getRegistrationNumber()).isPresent()) {
            throw new BadRequestException("Registration number already exists");
        }

        if (request.getBusNumber() != null) {
            bus.setBusNumber(request.getBusNumber());
        }
        if (request.getRegistrationNumber() != null) {
            bus.setRegistrationNumber(request.getRegistrationNumber());
        }
        if (request.getDriverId() != null) {
            User driver = userRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                    request.getDriverId(), schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
            bus.setDriver(driver);
        }
        if (request.getDeviceId() != null) {
            bus.setDeviceId(request.getDeviceId());
        }
        if (request.getCapacity() != null) {
            bus.setCapacity(request.getCapacity());
        }
        if (request.getIsActive() != null) {
            bus.setIsActive(request.getIsActive());
        }

        bus = busRepository.save(bus);

        return mapToBusResponse(bus);
    }

    @Transactional
    public void deleteBus(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Bus bus = busRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        busRepository.delete(bus);
    }

    // ============= ROUTE MANAGEMENT =============

    @Transactional
    public BusRouteResponse createRoute(CreateBusRouteRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        Bus bus = busRepository.findByIdAndSchool_IdAndDeletedAtIsNull(request.getBusId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        BusRoute route = BusRoute.builder()
                .school(school)
                .bus(bus)
                .routeName(request.getRouteName())
                .routeType(request.getRouteType())
                .stops(request.getStops())
                .isActive(true)
                .build();

        route = busRouteRepository.save(route);

        return mapToBusRouteResponse(route);
    }

    @Transactional(readOnly = true)
    public List<BusRouteResponse> getRoutes(UUID busId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<BusRoute> routes;
        if (busId != null) {
            routes = busRouteRepository.findBySchool_IdAndBus_IdAndDeletedAtIsNull(schoolId, busId);
        } else {
            routes = busRouteRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        }

        return routes.stream()
                .map(this::mapToBusRouteResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BusRouteResponse updateRoute(UUID id, UpdateBusRouteRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        BusRoute route = busRouteRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        if (request.getRouteName() != null) {
            route.setRouteName(request.getRouteName());
        }
        if (request.getRouteType() != null) {
            route.setRouteType(request.getRouteType());
        }
        if (request.getStops() != null) {
            route.setStops(request.getStops());
        }
        if (request.getIsActive() != null) {
            route.setIsActive(request.getIsActive());
        }

        route = busRouteRepository.save(route);

        return mapToBusRouteResponse(route);
    }

    @Transactional
    public void deleteRoute(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        BusRoute route = busRouteRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        busRouteRepository.delete(route);
    }

    // ============= TRIP MANAGEMENT =============

    @Transactional
    public BusTripResponse startTrip(UUID busId, StartTripRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Bus bus = busRepository.findByIdAndSchool_IdAndDeletedAtIsNull(busId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        // Check if there's already an active trip
        busTripRepository.findByBus_IdAndStatusAndDeletedAtIsNull(busId, BusTripStatus.IN_PROGRESS)
                .ifPresent(trip -> {
                    throw new BadRequestException("Bus already has an active trip");
                });

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        BusRoute route = null;
        if (request.getRouteId() != null) {
            route = busRouteRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                    request.getRouteId(), schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        }

        BusTrip trip = BusTrip.builder()
                .school(school)
                .bus(bus)
                .route(route)
                .tripType(request.getTripType())
                .status(BusTripStatus.IN_PROGRESS)
                .lastReachedStopOrder(0)
                .startTime(LocalDateTime.now())
                .build();

        trip = busTripRepository.save(trip);

        // Emit WebSocket event
        try {
            messagingTemplate.convertAndSend("/topic/bus/" + busId + "/trip-started",
                    mapToBusTripResponse(trip));
        } catch (Exception e) {
            log.error("Failed to emit trip started event", e);
        }

        return mapToBusTripResponse(trip);
    }

    @Transactional
    public BusTripResponse endTrip(UUID busId, EndTripRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        BusTrip trip = busTripRepository.findByBus_IdAndStatusAndDeletedAtIsNull(
                busId, BusTripStatus.IN_PROGRESS)
                .orElseThrow(() -> new BadRequestException("No active trip found for this bus"));

        if (!trip.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Trip not found");
        }

        trip.setStatus(BusTripStatus.COMPLETED);
        trip.setEndTime(LocalDateTime.now());
        trip.setNotes(request.getNotes());

        trip = busTripRepository.save(trip);

        // Emit WebSocket event
        try {
            messagingTemplate.convertAndSend("/topic/bus/" + busId + "/trip-ended",
                    mapToBusTripResponse(trip));
        } catch (Exception e) {
            log.error("Failed to emit trip ended event", e);
        }

        return mapToBusTripResponse(trip);
    }

    @Transactional(readOnly = true)
    public List<BusTripResponse> getTrips(UUID busId, BusTripStatus status,
            LocalDateTime startDate, LocalDateTime endDate) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<BusTrip> trips = busTripRepository.findByFilters(schoolId, busId, status, startDate, endDate);

        return trips.stream()
                .map(this::mapToBusTripResponse)
                .collect(Collectors.toList());
    }

    // ============= LOCATION TRACKING =============

    @Transactional
    public BusLocationResponse updateLocation(UpdateBusLocationRequest request) {
        Bus bus = busRepository.findByIdAndDeletedAtIsNull(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        // Get active trip
        BusTrip activeTrip = busTripRepository.findByBus_IdAndStatusAndDeletedAtIsNull(
                request.getBusId(), BusTripStatus.IN_PROGRESS)
                .orElse(null);

        // Deduplication: Check last location update (within 5 seconds)
        BusLocation lastLocation = busLocationRepository.findFirstByBus_IdOrderByTimestampDesc(request.getBusId())
                .orElse(null);

        if (lastLocation != null &&
                lastLocation.getTimestamp().plusSeconds(5).isAfter(LocalDateTime.now())) {
            log.debug("Location update ignored - too frequent (< 5 seconds)");
            return mapToBusLocationResponse(lastLocation);
        }

        BusLocation location = BusLocation.builder()
                .bus(bus)
                .trip(activeTrip)
                .lat(request.getLat())
                .lng(request.getLng())
                .speed(request.getSpeed())
                .heading(request.getHeading())
                .accuracy(request.getAccuracy())
                .timestamp(LocalDateTime.now())
                .build();

        location = busLocationRepository.save(location);

        // Emit WebSocket event
        try {
            LiveBusLocationResponse liveLocation = LiveBusLocationResponse.builder()
                    .bus(mapToBusResponse(bus))
                    .location(mapToBusLocationResponse(location))
                    .activeTrip(activeTrip != null ? mapToBusTripResponse(activeTrip) : null)
                    .build();

            messagingTemplate.convertAndSend("/topic/bus/" + request.getBusId() + "/location",
                    liveLocation);
        } catch (Exception e) {
            log.error("Failed to emit location update", e);
        }

        return mapToBusLocationResponse(location);
    }

    @Transactional(readOnly = true)
    public LiveBusLocationResponse getLiveLocation(UUID busId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Bus bus = busRepository.findByIdAndSchool_IdAndDeletedAtIsNull(busId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        // Check authorization: Admin or parent with child assigned to this bus
        if (!SecurityUtils.isAdmin()) {
            boolean hasAccess = studentBusAssignmentRepository
                    .findBySchool_IdAndBus_IdAndIsActiveTrueAndDeletedAtIsNull(schoolId, busId)
                    .stream()
                    .anyMatch(assignment -> {
                        Student student = assignment.getStudent();
                        return student.getParent() != null &&
                                student.getParent().getUser().getId().equals(currentUserId);
                    });

            if (!hasAccess) {
                throw new UnauthorizedException("You don't have access to this bus location");
            }
        }

        BusLocation location = busLocationRepository.findFirstByBus_IdOrderByTimestampDesc(busId)
                .orElse(null);

        BusTrip activeTrip = busTripRepository.findByBus_IdAndStatusAndDeletedAtIsNull(
                busId, BusTripStatus.IN_PROGRESS)
                .orElse(null);

        return LiveBusLocationResponse.builder()
                .bus(mapToBusResponse(bus))
                .location(location != null ? mapToBusLocationResponse(location) : null)
                .activeTrip(activeTrip != null ? mapToBusTripResponse(activeTrip) : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BusLocationResponse> getLocationHistory(UUID tripId) {
        List<BusLocation> locations = busLocationRepository.findByTrip_IdOrderByTimestampAsc(tripId);

        return locations.stream()
                .map(this::mapToBusLocationResponse)
                .collect(Collectors.toList());
    }

    // ============= STUDENT ASSIGNMENT =============

    @Transactional
    public StudentBusAssignmentResponse assignStudent(AssignStudentToBusRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                request.getStudentId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Bus bus = busRepository.findByIdAndSchool_IdAndDeletedAtIsNull(request.getBusId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        // Check if student already has an active assignment
        studentBusAssignmentRepository.findByStudent_IdAndDeletedAtIsNull(request.getStudentId())
                .ifPresent(existing -> {
                    if (existing.getIsActive()) {
                        throw new BadRequestException("Student already has an active bus assignment");
                    }
                });

        BusRoute route = null;
        if (request.getRouteId() != null) {
            route = busRouteRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                    request.getRouteId(), schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        }

        StudentBusAssignment assignment = StudentBusAssignment.builder()
                .school(school)
                .student(student)
                .bus(bus)
                .route(route)
                .stopName(request.getStopName())
                .pickupTime(request.getPickupTime())
                .dropoffTime(request.getDropoffTime())
                .isActive(true)
                .build();

        assignment = studentBusAssignmentRepository.save(assignment);

        return mapToStudentBusAssignmentResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<StudentBusAssignmentResponse> getAssignments(UUID busId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<StudentBusAssignment> assignments;
        if (busId != null) {
            assignments = studentBusAssignmentRepository
                    .findBySchool_IdAndBus_IdAndIsActiveTrueAndDeletedAtIsNull(schoolId, busId);
        } else {
            assignments = studentBusAssignmentRepository
                    .findBySchool_IdAndIsActiveTrueAndDeletedAtIsNull(schoolId);
        }

        return assignments.stream()
                .map(this::mapToStudentBusAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void unassignStudent(UUID studentId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudentBusAssignment assignment = studentBusAssignmentRepository
                .findByStudent_IdAndSchool_IdAndDeletedAtIsNull(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        studentBusAssignmentRepository.delete(assignment);
    }

    // ============= MAPPERS =============

    private BusResponse mapToBusResponse(Bus bus) {
        UserResponse driverInfo = null;
        if (bus.getDriver() != null) {
            User driver = bus.getDriver();
            driverInfo = UserResponse.builder()
                    .id(driver.getId())
                    .name(driver.getName())
                    .email(driver.getEmail())
                    .phone(driver.getPhone())
                    .role(driver.getRole())
                    .isActive(driver.getIsActive())
                    .schoolId(driver.getSchool() != null ? driver.getSchool().getId() : null)
                    .build();
        }

        return BusResponse.builder()
                .id(bus.getId())
                .busNumber(bus.getBusNumber())
                .registrationNumber(bus.getRegistrationNumber())
                .capacity(bus.getCapacity())
                .isActive(bus.getIsActive())
                .deviceId(bus.getDeviceId())
                .driverId(bus.getDriver() != null ? bus.getDriver().getId() : null)
                .driver(driverInfo)
                .build();
    }

    private BusRouteResponse mapToBusRouteResponse(BusRoute route) {
        return BusRouteResponse.builder()
                .id(route.getId())
                .routeName(route.getRouteName())
                .routeType(route.getRouteType())
                .stops(route.getStops())
                .isActive(route.getIsActive())
                .busId(route.getBus().getId())
                .busNumber(route.getBus().getBusNumber())
                .build();
    }

    private BusTripResponse mapToBusTripResponse(BusTrip trip) {
        return BusTripResponse.builder()
                .id(trip.getId())
                .tripType(trip.getTripType())
                .status(trip.getStatus())
                .lastReachedStopOrder(trip.getLastReachedStopOrder())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .notes(trip.getNotes())
                .busId(trip.getBus().getId())
                .routeId(trip.getRoute() != null ? trip.getRoute().getId() : null)
                .build();
    }

    private BusLocationResponse mapToBusLocationResponse(BusLocation location) {
        return BusLocationResponse.builder()
                .id(location.getId())
                .lat(location.getLat())
                .lng(location.getLng())
                .speed(location.getSpeed())
                .heading(location.getHeading())
                .accuracy(location.getAccuracy())
                .timestamp(location.getTimestamp())
                .busId(location.getBus().getId())
                .tripId(location.getTrip() != null ? location.getTrip().getId() : null)
                .build();
    }

    private StudentBusAssignmentResponse mapToStudentBusAssignmentResponse(StudentBusAssignment assignment) {
        return StudentBusAssignmentResponse.builder()
                .id(assignment.getId())
                .stopName(assignment.getStopName())
                .pickupTime(assignment.getPickupTime())
                .dropoffTime(assignment.getDropoffTime())
                .isActive(assignment.getIsActive())
                .studentId(assignment.getStudent().getId())
                .studentName(assignment.getStudent().getName())
                .admissionNumber(assignment.getStudent().getAdmissionNumber())
                .busId(assignment.getBus().getId())
                .busNumber(assignment.getBus().getBusNumber())
                .routeId(assignment.getRoute() != null ? assignment.getRoute().getId() : null)
                .routeName(assignment.getRoute() != null ? assignment.getRoute().getRouteName() : null)
                .build();
    }
}