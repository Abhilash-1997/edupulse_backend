package com.school.management.service;

import com.school.management.constant.AnnouncementPriority;
import com.school.management.constant.UserRole;
import com.school.management.dto.request.CreateAnnouncementRequest;
import com.school.management.dto.request.UpdateAnnouncementRequest;
import com.school.management.dto.response.AnnouncementResponse;
import com.school.management.entity.Announcement;
import com.school.management.entity.ClassEntity;
import com.school.management.entity.School;
import com.school.management.entity.User;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.AnnouncementRepository;
import com.school.management.repository.ClassRepository;
import com.school.management.repository.SchoolRepository;
import com.school.management.repository.UserRepository;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;

    /**
     * Create announcement
     */
    @Transactional
    public List<AnnouncementResponse> createAnnouncement(CreateAnnouncementRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UserRole currentRole = SecurityUtils.getCurrentUserRole();

        User author = userRepository.findByIdAndDeletedAtIsNull(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<AnnouncementResponse> responses = new ArrayList<>();

        // Check for duplicate announcement (same title + message)
        List<Announcement> duplicates = announcementRepository
                .findByAuthor_IdAndTitleAndMessageAndDeletedAtIsNull(
                        currentUserId, request.getTitle(), request.getMessage());

        if (!duplicates.isEmpty()) {
            throw new BadRequestException("Duplicate announcement. Same title and message already exists.");
        }

        // SUPER_ADMIN: Can create for multiple schools
        if (currentRole == UserRole.SUPER_ADMIN &&
                request.getTargetSchoolIds() != null &&
                !request.getTargetSchoolIds().isEmpty()) {

            for (UUID targetSchoolId : request.getTargetSchoolIds()) {
                School school = schoolRepository.findByIdAndDeletedAtIsNull(targetSchoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("School not found: " + targetSchoolId));

                Announcement announcement = buildAnnouncement(request, author, school, currentRole, null);
                announcement = announcementRepository.save(announcement);

                responses.add(mapToAnnouncementResponse(announcement));
            }
        } else {
            // Normal flow: create for current school
            School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("School not found"));

            ClassEntity targetClass = null;
            if (request.getTargetClassId() != null) {
                targetClass = classRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                request.getTargetClassId(), schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
            }

            Announcement announcement = buildAnnouncement(request, author, school, currentRole, targetClass);
            announcement = announcementRepository.save(announcement);

            responses.add(mapToAnnouncementResponse(announcement));
        }

        return responses;
    }

    /**
     * Get all announcements (active only for non-admin)
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncements() {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Announcement> announcements;

        if (SecurityUtils.isAdmin()) {
            // Admin sees all announcements
            announcements = announcementRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        } else {
            // Regular users see only active announcements
            announcements = announcementRepository.findActiveAnnouncementsBySchool(
                    schoolId, LocalDateTime.now());
        }

        return announcements.stream()
                .map(this::mapToAnnouncementResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get announcement by ID
     */
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(UUID id) {
        Announcement announcement = announcementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        return mapToAnnouncementResponse(announcement);
    }

    /**
     * Update announcement
     */
    @Transactional
    public AnnouncementResponse updateAnnouncement(UUID id, UpdateAnnouncementRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Announcement announcement = announcementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        // For SUPER_ADMIN, allow updating any announcement
        // For others, verify it belongs to their school
        if (!SecurityUtils.isSuperAdmin()) {
            if (!announcement.getSchool().getId().equals(schoolId)) {
                throw new ResourceNotFoundException("Announcement not found");
            }
        }

        if (request.getTitle() != null) {
            announcement.setTitle(request.getTitle());
        }
        if (request.getMessage() != null) {
            announcement.setMessage(request.getMessage());
        }
        if (request.getPriority() != null) {
            announcement.setPriority(request.getPriority());
        }
        if (request.getExpiryDate() != null) {
            announcement.setExpiryDate(request.getExpiryDate());
        }

        announcement = announcementRepository.save(announcement);

        return mapToAnnouncementResponse(announcement);
    }

    /**
     * Delete announcement
     */
    @Transactional
    public void deleteAnnouncement(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Announcement announcement = announcementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        // For SUPER_ADMIN, allow deleting any announcement
        // For others, verify it belongs to their school
        if (!SecurityUtils.isSuperAdmin()) {
            if (!announcement.getSchool().getId().equals(schoolId)) {
                throw new ResourceNotFoundException("Announcement not found");
            }
        }

        announcementRepository.delete(announcement);
    }

    private Announcement buildAnnouncement(
            CreateAnnouncementRequest request,
            User author,
            School school,
            UserRole authorRole,
            ClassEntity targetClass) {

        return Announcement.builder()
                .school(school)
                .author(author)
                .authorRole(authorRole)
                .classEntity(targetClass)
                .title(request.getTitle())
                .message(request.getMessage())
                .priority(request.getPriority() != null ?
                        request.getPriority() : AnnouncementPriority.MEDIUM)
                .expiryDate(request.getExpiryDate())
                .isActive(true)
                .build();
    }

    private AnnouncementResponse mapToAnnouncementResponse(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .message(announcement.getMessage())
                .priority(announcement.getPriority())
                .expiryDate(announcement.getExpiryDate())
                .isActive(announcement.getIsActive())
                .authorRole(announcement.getAuthorRole())
                .authorId(announcement.getAuthor().getId())
                .authorName(announcement.getAuthor().getName())
                .schoolId(announcement.getSchool().getId())
                .classId(announcement.getClassEntity() != null ?
                        announcement.getClassEntity().getId() : null)
                .className(announcement.getClassEntity() != null ?
                        announcement.getClassEntity().getName() : null)
                .createdAt(announcement.getCreatedAt())
                .build();
    }
}