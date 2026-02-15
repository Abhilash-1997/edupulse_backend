package com.school.management.service;

import com.school.management.dto.request.MarkAttendanceRequest;
import com.school.management.dto.request.UpdateAttendanceRequest;
import com.school.management.dto.response.AttendanceResponse;
import com.school.management.dto.response.AttendanceStatusResponse;
import com.school.management.dto.response.StudentResponse;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    /**
     * Mark attendance for students (bulk operation)
     * Implements upsert logic - updates existing records or creates new ones
     */
    @Transactional
    public List<AttendanceResponse> markAttendance(MarkAttendanceRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        ClassSection section = classSectionRepository.findByIdAndDeletedAtIsNull(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        User recorder = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<MarkAttendanceRequest.StudentAttendance> attendanceList =
                request.getAttendance() != null ? request.getAttendance() : request.getStudents();

        if (attendanceList == null || attendanceList.isEmpty()) {
            throw new BadRequestException("Invalid students data");
        }

        List<AttendanceResponse> responses = new ArrayList<>();

        // Bulk upsert operation
        for (MarkAttendanceRequest.StudentAttendance studentAtt : attendanceList) {
            // Find or create attendance record (upsert logic)
            Attendance attendance = attendanceRepository
                    .findBySchool_IdAndStudent_IdAndDateAndDeletedAtIsNull(
                            schoolId,
                            studentAtt.getStudentId(),
                            request.getDate()
                    )
                    .orElse(Attendance.builder()
                            .school(school)
                            .section(section)
                            .date(request.getDate())
                            .recordedBy(recorder)
                            .isLocked(false)
                            .build());

            Student student = studentRepository.findByIdAndDeletedAtIsNull(studentAtt.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
            attendance.setStudent(student);
            attendance.setStatus(studentAtt.getStatus());
            attendance.setRecordedBy(recorder); // Update recorder even on update

            attendance = attendanceRepository.save(attendance);

            responses.add(mapToAttendanceResponse(attendance));
        }

        log.info("Attendance marked for {} students on {}", responses.size(), request.getDate());

        return responses;
    }

    /**
     * Get attendance with marked and pending students
     * Returns two lists: already marked attendance and students pending attendance
     */
    @Transactional(readOnly = true)
    public AttendanceStatusResponse getAttendance(UUID sectionId, LocalDate date) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        if (sectionId == null || date == null) {
            throw new BadRequestException("Section ID and Date are required");
        }

        // 1. Fetch all students in the section
        List<Student> allStudents = studentRepository.findBySection_IdAndSchool_IdAndDeletedAtIsNull(
                sectionId, schoolId);

        // 2. Fetch existing attendance for the date
        List<Attendance> markedAttendance = attendanceRepository
                .findBySchool_IdAndSection_IdAndDateAndDeletedAtIsNull(schoolId, sectionId, date);

        // 3. Separate into Marked and Pending
        Set<UUID> markedStudentIds = markedAttendance.stream()
                .map(att -> att.getStudent().getId())
                .collect(Collectors.toSet());

        List<Student> pendingStudents = allStudents.stream()
                .filter(student -> !markedStudentIds.contains(student.getId()))
                .collect(Collectors.toList());

        return AttendanceStatusResponse.builder()
                .marked(markedAttendance.stream()
                        .map(this::mapToAttendanceResponse)
                        .collect(Collectors.toList()))
                .pending(pendingStudents.stream()
                        .map(this::mapToStudentResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Update single attendance record
     */
    @Transactional
    public AttendanceResponse updateAttendance(UUID id, UpdateAttendanceRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Attendance attendance = attendanceRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));

        if (attendance.getIsLocked()) {
            throw new BadRequestException("Attendance is locked and cannot be modified");
        }

        attendance.setStatus(request.getStatus());
        attendance = attendanceRepository.save(attendance);

        return mapToAttendanceResponse(attendance);
    }

    /**
     * Get attendance report with filters
     * Supports filtering by section, student, date, and date range
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceReport(UUID sectionId, UUID studentId,
                                                        LocalDate date, LocalDate startDate,
                                                        LocalDate endDate) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Attendance> attendances;

        // Apply filters based on provided parameters
        if (date != null) {
            // Specific date
            if (sectionId != null && studentId != null) {
                // Section + Student + Date
                attendances = attendanceRepository
                        .findBySchool_IdAndSection_IdAndDateAndDeletedAtIsNull(schoolId, sectionId, date)
                        .stream()
                        .filter(a -> a.getStudent().getId().equals(studentId))
                        .collect(Collectors.toList());
            } else if (sectionId != null) {
                // Section + Date
                attendances = attendanceRepository
                        .findBySchool_IdAndSection_IdAndDateAndDeletedAtIsNull(schoolId, sectionId, date);
            } else if (studentId != null) {
                // Student + Date (fetch all and filter)
                attendances = attendanceRepository
                        .findBySchool_IdAndDateAndDeletedAtIsNull(schoolId, date)
                        .stream()
                        .filter(a -> a.getStudent().getId().equals(studentId))
                        .collect(Collectors.toList());
            } else {
                // Just Date
                attendances = attendanceRepository
                        .findBySchool_IdAndDateAndDeletedAtIsNull(schoolId, date);
            }
        } else if (startDate != null && endDate != null) {
            // Date range
            attendances = attendanceRepository
                    .findBySchoolIdAndDateRange(schoolId, startDate, endDate);

            // Apply additional filters
            if (sectionId != null) {
                UUID finalSectionId = sectionId;
                attendances = attendances.stream()
                        .filter(a -> a.getSection().getId().equals(finalSectionId))
                        .collect(Collectors.toList());
            }
            if (studentId != null) {
                UUID finalStudentId = studentId;
                attendances = attendances.stream()
                        .filter(a -> a.getStudent().getId().equals(finalStudentId))
                        .collect(Collectors.toList());
            }
        } else {
            // No date filter - get by section or student
            if (sectionId != null) {
                attendances = attendanceRepository
                        .findBySchool_IdAndSection_IdAndDeletedAtIsNull(schoolId, sectionId);
            } else if (studentId != null) {
                attendances = attendanceRepository
                        .findBySchool_IdAndDeletedAtIsNull(schoolId)
                        .stream()
                        .filter(a -> a.getStudent().getId().equals(studentId))
                        .collect(Collectors.toList());
            } else {
                // All attendance for school
                attendances = attendanceRepository
                        .findBySchool_IdAndDeletedAtIsNull(schoolId);
            }
        }

        return attendances.stream()
                .map(this::mapToAttendanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lock attendance for a specific section and date
     * Prevents further modifications
     */
    @Transactional
    public void lockAttendance(UUID sectionId, LocalDate date) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        List<Attendance> attendances = attendanceRepository.findBySchool_IdAndSection_IdAndDateAndDeletedAtIsNull(schoolId, sectionId, date);
        if (attendances.isEmpty()) {
            throw new ResourceNotFoundException("No attendance records found for this section and date");
        }
        for (Attendance attendance : attendances) {
            attendance.setIsLocked(true);
        }
        attendanceRepository.saveAll(attendances);
        log.info("Locked {} attendance records for section {} on {}",
                attendances.size(), sectionId, date);
    }

    // ============= MAPPERS =============

    private AttendanceResponse mapToAttendanceResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent() != null && attendance.getStudent().getName() != null ?
                        attendance.getStudent().getName() : null)
                .admissionNumber(attendance.getStudent() != null &&
                        attendance.getStudent().getAdmissionNumber() != null ?
                        attendance.getStudent().getAdmissionNumber() : null)
                .recordedBy(attendance.getRecordedBy() != null ?
                        attendance.getRecordedBy().getId() : null)
                .isLocked(attendance.getIsLocked())
                .build();
    }

    private StudentResponse mapToStudentResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .admissionNumber(student.getAdmissionNumber())
                .profilePicture(student.getProfilePicture())
                .build();
    }
}