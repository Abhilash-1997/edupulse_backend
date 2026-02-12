package com.school.management.service;

import com.school.management.constant.LeaveStatus;
import com.school.management.constant.UserRole;
import com.school.management.dto.request.ApplyLeaveRequest;
import com.school.management.dto.request.UpdateLeaveStatusRequest;
import com.school.management.dto.response.LeaveResponse;
import com.school.management.dto.response.UserResponse;
import com.school.management.entity.Leave;
import com.school.management.entity.School;
import com.school.management.entity.User;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.LeaveRepository;
import com.school.management.repository.SchoolRepository;
import com.school.management.repository.UserRepository;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final EmailService emailService;

    /**
     * Apply for leave
     */
    @Transactional
    public LeaveResponse applyLeave(ApplyLeaveRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UserRole userRole = SecurityUtils.getCurrentUserRole();

        User applicant = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Leave leave = Leave.builder()
                .school(school)
                .applicant(applicant)
                .role(userRole.name())
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        leave = leaveRepository.save(leave);

        // Send notification email to admin (best effort)
        try {
            emailService.sendLeaveApplicationEmail(
                    school.getName() + " Admin",
                    applicant.getName(),
                    request.getType().name(),
                    request.getStartDate().toString(),
                    request.getEndDate().toString(),
                    request.getReason(),
                    school.getName()
            );
        } catch (Exception e) {
            log.error("Failed to send leave application email", e);
        }

        return mapToLeaveResponse(leave);
    }

    /**
     * Get all leaves with optional filters
     */
    @Transactional(readOnly = true)
    public List<LeaveResponse> getLeaves(LeaveStatus status, String role) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UserRole currentRole = SecurityUtils.getCurrentUserRole();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        List<Leave> leaves;

        // Admin can see all leaves, others only their own
        if (SecurityUtils.isAdmin()) {
            if (status != null && role != null) {
                leaves = leaveRepository.findBySchool_IdAndStatusAndRoleAndDeletedAtIsNull(
                        schoolId, status, role);
            } else if (status != null) {
                leaves = leaveRepository.findBySchool_IdAndStatusAndDeletedAtIsNull(schoolId, status);
            } else if (role != null) {
                leaves = leaveRepository.findBySchool_IdAndRoleAndDeletedAtIsNull(schoolId, role);
            } else {
                leaves = leaveRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
            }
        } else {
            // Non-admin users see only their own leaves
            leaves = leaveRepository.findByApplicant_IdAndDeletedAtIsNull(currentUserId);

            // Apply filters
            if (status != null) {
                leaves = leaves.stream()
                        .filter(l -> l.getStatus() == status)
                        .collect(Collectors.toList());
            }
        }

        return leaves.stream()
                .map(this::mapToLeaveResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyLeaves(LeaveStatus status) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<Leave> leaves = leaveRepository.findByApplicant_IdAndDeletedAtIsNull(currentUserId);
        if (status != null) {
            leaves = leaves.stream().filter(l -> l.getStatus() == status)
                    .toList();
        }
        return leaves.stream().map(this::mapToLeaveResponse).toList();
    }


    @Transactional(readOnly = true)
    public List<LeaveResponse> getAllLeaves(LeaveStatus status, String role) {

        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        if (!SecurityUtils.isAdmin()) {
            throw new BadRequestException("Only admin can view all leaves");
        }

        List<Leave> leaves;

        if (status != null && role != null) {
            leaves = leaveRepository.findBySchool_IdAndStatusAndRoleAndDeletedAtIsNull(schoolId, status, role);
        } else if (status != null) {
            leaves = leaveRepository.findBySchool_IdAndStatusAndDeletedAtIsNull(schoolId, status);
        } else if (role != null) {
            leaves = leaveRepository.findBySchool_IdAndRoleAndDeletedAtIsNull(schoolId, role);
        } else {
            leaves = leaveRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        }

        return leaves.stream()
                .map(this::mapToLeaveResponse)
                .toList();
    }




    /**
     * Get leave by ID
     */
    @Transactional(readOnly = true)
    public LeaveResponse getLeavesById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Leave leave = leaveRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        // Check authorization
        if (!SecurityUtils.isAdmin() && !leave.getApplicant().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Leave not found");
        }

        return mapToLeaveResponse(leave);
    }

    /**
     * Update leave status (Approve/Reject)
     */
    @Transactional
    public LeaveResponse updateLeaveStatus(UUID id, UpdateLeaveStatusRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Leave leave = leaveRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leaves can be updated");
        }

        leave.setStatus(request.getStatus());
        leave = leaveRepository.save(leave);

        // Send notification to applicant
        try {
            emailService.sendLeaveStatusEmail(
                    leave.getApplicant().getEmail(),
                    leave.getApplicant().getName(),
                    leave.getType().name(),
                    request.getStatus().name(),
                    leave.getSchool().getName()
            );
        } catch (Exception e) {
            log.error("Failed to send leave status email", e);
        }

        return mapToLeaveResponse(leave);
    }

    /**
     * Delete leave application
     */
    @Transactional
    public void deleteLeave(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Leave leave = leaveRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        // Only applicant or admin can delete
        if (!SecurityUtils.isAdmin() && !leave.getApplicant().getId().equals(currentUserId)) {
            throw new BadRequestException("You can only delete your own leave applications");
        }

        // Can only delete pending leaves
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leaves can be deleted");
        }

        leaveRepository.delete(leave);
    }

    private LeaveResponse mapToLeaveResponse(Leave leave) {
        return LeaveResponse.builder()
                .id(leave.getId())
                .type(leave.getType())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .reason(leave.getReason())
                .status(leave.getStatus())
                .role(leave.getRole())
                .applicantId(leave.getApplicant().getId())
                .applicant(mapToUserResponse(leave.getApplicant()))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}