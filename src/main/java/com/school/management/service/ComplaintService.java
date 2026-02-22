package com.school.management.service;

import com.school.management.constant.ComplaintPriority;
import com.school.management.constant.ComplaintStatus;
import com.school.management.dto.request.CreateComplaintRequest;
import com.school.management.dto.response.ComplaintResponse;
import com.school.management.entity.Complaint;
import com.school.management.entity.Parent;
import com.school.management.entity.School;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.ComplaintRepository;
import com.school.management.repository.ParentRepository;
import com.school.management.repository.SchoolRepository;
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
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ParentRepository parentRepository;
    private final SchoolRepository schoolRepository;
    private final EmailService emailService;

    /**
     * Create complaint (Parent only)
     */
    @Transactional
    public ComplaintResponse createComplaint(CreateComplaintRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Parent parent = parentRepository.findByUser_IdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found"));

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        Complaint complaint = Complaint.builder()
                .school(school)
                .parent(parent)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ComplaintStatus.Open)
                .priority(request.getPriority() != null ?
                        request.getPriority() : ComplaintPriority.Medium)
                .build();

        complaint = complaintRepository.save(complaint);

        // Send acknowledgment email (best effort)
        try {
            emailService.sendComplaintAcknowledgmentEmail(
                    parent.getUser().getEmail(),
                    parent.getGuardianName(),
                    request.getTitle(),
                    complaint.getId().toString(),
                    school.getName()
            );
        } catch (Exception e) {
            log.error("Failed to send complaint acknowledgment email", e);
        }

        return mapToComplaintResponse(complaint);
    }

    /**
     * Get all complaints with optional filters
     */
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getComplaints(ComplaintStatus status, ComplaintPriority priority) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        List<Complaint> complaints;

        // Admin and Teacher sees all complaints
        if (status != null && priority != null) {
            complaints = complaintRepository.findBySchool_IdAndStatusAndPriorityAndDeletedAtIsNull(
                    schoolId, status, priority);
        } else if (status != null) {
            complaints = complaintRepository.findBySchool_IdAndStatusAndDeletedAtIsNull(
                    schoolId, status);
        } else if (priority != null) {
            complaints = complaintRepository.findBySchool_IdAndPriorityAndDeletedAtIsNull(
                    schoolId, priority);
        } else {
            complaints = complaintRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        }
        return complaints.stream()
                .map(this::mapToComplaintResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> myComplaints(ComplaintStatus status, ComplaintPriority priority){
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Parent parent = parentRepository.findByUser_IdAndDeletedAtIsNull(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found"));

        List<Complaint> complaints = complaintRepository.findByParent_IdAndDeletedAtIsNull(parent.getId());

        // Apply filters
        if (status != null) {
            complaints = complaints.stream().filter(c -> c.getStatus() == status)
                    .collect(Collectors.toList());
        }
        if (priority != null) {
            complaints = complaints.stream().filter(c -> c.getPriority() == priority)
                    .collect(Collectors.toList());
        }
        return complaints.stream()
                .map(this::mapToComplaintResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get complaint by ID
     */
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Complaint complaint = complaintRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        // Verify access (admin or complaint owner)
        if (!SecurityUtils.isAdmin()) {
            Parent parent = parentRepository.findByUser_IdAndDeletedAtIsNull(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found"));

            if (!complaint.getParent().getId().equals(parent.getId())) {
                throw new ResourceNotFoundException("Complaint not found");
            }
        }

        return mapToComplaintResponse(complaint);
    }

    /**
     * Update complaint status (Admin only)
     */
    @Transactional
    public ComplaintResponse updateComplaintStatus(UUID id, ComplaintStatus status) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Complaint complaint = complaintRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (!complaint.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Complaint not found");
        }

        complaint.setStatus(status);
        complaint = complaintRepository.save(complaint);

        // Send status update email (best effort)
        try {
            emailService.sendComplaintStatusEmail(
                    complaint.getParent().getUser().getEmail(),
                    complaint.getParent().getGuardianName(),
                    complaint.getTitle(),
                    status.getDisplayName(),
                    complaint.getSchool().getName()
            );
        } catch (Exception e) {
            log.error("Failed to send complaint status email", e);
        }

        return mapToComplaintResponse(complaint);
    }

    /**
     * Delete complaint (Admin only)
     */
    @Transactional
    public void deleteComplaint(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Complaint complaint = complaintRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (!complaint.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Complaint not found");
        }

        complaintRepository.delete(complaint);
    }

    private ComplaintResponse mapToComplaintResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .parentId(complaint.getParent().getId())
                .guardianName(complaint.getParent().getGuardianName())
                .occupation(complaint.getParent().getOccupation())
                .createdAt(complaint.getCreatedAt())
                .build();
    }
}