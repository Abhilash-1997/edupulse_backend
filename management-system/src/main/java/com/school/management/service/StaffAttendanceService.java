package com.school.management.service;

import com.school.management.constant.AttendanceStatus;
import com.school.management.dto.request.MarkStaffAttendanceRequest;
import com.school.management.dto.response.StaffAttendanceResponse;
import com.school.management.entity.School;
import com.school.management.entity.StaffAttendance;
import com.school.management.entity.StaffProfile;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.SchoolRepository;
import com.school.management.repository.StaffAttendanceRepository;
import com.school.management.repository.StaffProfileRepository;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffAttendanceService {

    private final StaffAttendanceRepository staffAttendanceRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final SchoolRepository schoolRepository;

    /**
     * Mark attendance for staff members
     */
    @Transactional
    public List<StaffAttendanceResponse> markAttendance(MarkStaffAttendanceRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        List<StaffAttendanceResponse> responses = new ArrayList<>();

        for (MarkStaffAttendanceRequest.StaffAttendanceData data : request.getAttendanceData()) {
            // Verify staff exists
            StaffProfile staff = staffProfileRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                            data.getStaffId(), schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Staff not found: " + data.getStaffId()));

            // Find or create attendance record
            StaffAttendance attendance = staffAttendanceRepository
                    .findBySchool_IdAndStaff_IdAndDateAndDeletedAtIsNull(
                            schoolId, data.getStaffId(), request.getDate())
                    .orElse(StaffAttendance.builder()
                            .school(school)
                            .staff(staff)
                            .date(request.getDate())
                            .build());

            attendance.setStatus(data.getStatus());
            attendance.setCheckInTime(data.getCheckInTime());
            attendance.setCheckOutTime(data.getCheckOutTime());
            attendance.setRemarks(data.getRemarks());

            attendance = staffAttendanceRepository.save(attendance);

            responses.add(mapToStaffAttendanceResponse(attendance));
        }

        return responses;
    }

    /**
     * Get staff attendance for a specific date
     */
    @Transactional(readOnly = true)
    public List<StaffAttendanceResponse> getAttendanceByDate(LocalDate date) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<StaffAttendance> attendances = staffAttendanceRepository
                .findBySchool_IdAndDateAndDeletedAtIsNull(schoolId, date);

        return attendances.stream()
                .map(this::mapToStaffAttendanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get attendance for a specific staff member
     */
    @Transactional(readOnly = true)
    public List<StaffAttendanceResponse> getStaffAttendance(
            UUID staffId, LocalDate startDate, LocalDate endDate) {

        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        // Verify staff belongs to school
        staffProfileRepository.findByIdAndSchool_IdAndDeletedAtIsNull(staffId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        List<StaffAttendance> attendances = staffAttendanceRepository
                .findByStaffIdAndDateRange(staffId, startDate, endDate);

        return attendances.stream()
                .map(this::mapToStaffAttendanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update attendance record
     */
    @Transactional
    public StaffAttendanceResponse updateAttendance(UUID id, MarkStaffAttendanceRequest.StaffAttendanceData data) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StaffAttendance attendance = staffAttendanceRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));

        if (data.getStatus() != null) {
            attendance.setStatus(data.getStatus());
        }
        if (data.getCheckInTime() != null) {
            attendance.setCheckInTime(data.getCheckInTime());
        }
        if (data.getCheckOutTime() != null) {
            attendance.setCheckOutTime(data.getCheckOutTime());
        }
        if (data.getRemarks() != null) {
            attendance.setRemarks(data.getRemarks());
        }

        attendance = staffAttendanceRepository.save(attendance);

        return mapToStaffAttendanceResponse(attendance);
    }

    /**
     * Delete attendance record
     */
    @Transactional
    public void deleteAttendance(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StaffAttendance attendance = staffAttendanceRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));

        staffAttendanceRepository.delete(attendance);
    }

    private StaffAttendanceResponse mapToStaffAttendanceResponse(StaffAttendance attendance) {
        return StaffAttendanceResponse.builder()
                .id(attendance.getId())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .remarks(attendance.getRemarks())
                .staffId(attendance.getStaff().getId())
                .build();
    }
}