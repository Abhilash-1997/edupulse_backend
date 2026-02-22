package com.school.management.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.school.management.constant.StudyMaterialStatus;
import com.school.management.constant.StudyMaterialType;
import com.school.management.constant.UserRole;
import com.school.management.dto.request.CreateStudyMaterialSectionRequest;
import com.school.management.dto.request.UpdateStudyMaterialRequest;
import com.school.management.dto.request.UpdateStudyMaterialSectionRequest;
import com.school.management.dto.request.UploadStudyMaterialRequest;
import com.school.management.dto.response.HlsStreamResponse;
import com.school.management.dto.response.StreamTokenResponse;
import com.school.management.dto.response.StudyMaterialResponse;
import com.school.management.dto.response.StudyMaterialSectionResponse;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ForbiddenException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.exception.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.security.JwtTokenProvider;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyMaterialService {

    private final StudyMaterialSectionRepository studyMaterialSectionRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GcsService gcsService;
    private final VideoProcessingService videoProcessingService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ============= SECTION MANAGEMENT =============

    @Transactional
    public StudyMaterialSectionResponse createSection(CreateStudyMaterialSectionRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        User creator = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ClassEntity classEntity = classRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                request.getClassId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        ClassSection section = null;
        if (request.getSectionId() != null) {
            section = classSectionRepository
                    .findByIdAndDeletedAtIsNull(request.getSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

            if (!section.getClassEntity().getId().equals(request.getClassId())) {
                throw new BadRequestException("Section does not belong to the selected class");
            }
        }


        Subject subject = subjectRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                request.getSubjectId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        StudyMaterialSection materialSection = StudyMaterialSection.builder()
                .school(school)
                .classEntity(classEntity)
                .section(section)
                .subject(subject)
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .order(request.getOrder() != null ? request.getOrder() : 0)
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .build();

        materialSection = studyMaterialSectionRepository.save(materialSection);

        return mapToStudyMaterialSectionResponse(materialSection);
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialSectionResponse> getSections(UUID classId, UUID subjectId, UUID sectionId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();
        UserRole userRole = SecurityUtils.getCurrentUserRole();

        List<StudyMaterialSection> sections;

        if (UserRole.PARENT.equals(userRole)) {
            // Parents see published sections for their children's classes
            Parent parent = parentRepository
                    .findByUser_IdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new ForbiddenException("Parent record not found"));

            List<Student> students = studentRepository
                    .findByParent_IdAndDeletedAtIsNull(parent.getId());

            List<UUID> childClassIds = students.stream()
                    .filter(s -> s.getClassEntity() != null)
                    .map(s -> s.getClassEntity().getId())
                    .distinct()
                    .collect(Collectors.toList());

            sections = studyMaterialSectionRepository.findByFilters(
                    schoolId, classId, subjectId, sectionId);

            sections = sections.stream()
                    .filter(s -> s.getIsPublished() &&
                            (childClassIds.isEmpty() || childClassIds.contains(s.getClassEntity().getId())))
                    .collect(Collectors.toList());

        } else if (!SecurityUtils.isAdmin()) {
            // Teachers see all (filtered by provided params), students see only published
            sections = studyMaterialSectionRepository.findByFilters(
                    schoolId, classId, subjectId, sectionId);

            if (!SecurityUtils.hasRole(UserRole.TEACHER)) {
                sections = sections.stream()
                        .filter(StudyMaterialSection::getIsPublished)
                        .collect(Collectors.toList());
            }
        } else {
            // Admins see everything
            sections = studyMaterialSectionRepository.findByFilters(
                    schoolId, classId, subjectId, sectionId);
        }

        return sections.stream()
                .map(this::mapToStudyMaterialSectionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudyMaterialSectionResponse getSectionById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterialSection section = studyMaterialSectionRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        // Check access for non-admin users
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasRole(UserRole.TEACHER) && !section.getIsPublished()) {
            throw new ForbiddenException("This section is not published");
        }

        return mapToStudyMaterialSectionResponse(section);
    }

    @Transactional
    public StudyMaterialSectionResponse updateSection(UUID id, UpdateStudyMaterialSectionRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterialSection section = studyMaterialSectionRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        if (request.getTitle() != null) {
            section.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            section.setDescription(request.getDescription());
        }
        if (request.getOrder() != null) {
            section.setOrder(request.getOrder());
        }
        if (request.getIsPublished() != null) {
            section.setIsPublished(request.getIsPublished());
        }

        section = studyMaterialSectionRepository.save(section);

        return mapToStudyMaterialSectionResponse(section);
    }

    @Transactional
    public void deleteSection(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterialSection section = studyMaterialSectionRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        // Delete all materials and their GCS files in this section
        List<StudyMaterial> materials = studyMaterialRepository
                .findBySection_IdAndDeletedAtIsNull(section.getId());

        for (StudyMaterial material : materials) {
            // Delete from GCS
            if (material.getFilePath() != null) {
                gcsService.deleteFile(material.getFilePath());
            }
            // Delete HLS files if video
            if (material.getHlsPath() != null) {
                gcsService.deleteFile(material.getHlsPath());
            }
            // Delete thumbnail if exists
            if (material.getThumbnailPath() != null) {
                gcsService.deleteFile(material.getThumbnailPath());
            }
        }

        studyMaterialSectionRepository.delete(section);
    }

    @Transactional
    public StudyMaterialSectionResponse toggleSectionPublish(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterialSection section = studyMaterialSectionRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        section.setIsPublished(!section.getIsPublished());
        section = studyMaterialSectionRepository.save(section);

        return mapToStudyMaterialSectionResponse(section);
    }

    // ============= MATERIAL MANAGEMENT =============

    @Transactional
    public StudyMaterialResponse uploadMaterial(UUID sectionId, UploadStudyMaterialRequest request,
            MultipartFile file) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        User uploader = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        StudyMaterialSection section = studyMaterialSectionRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(sectionId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        // Detect file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("Invalid file name");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        StudyMaterialType materialType = detectMaterialType(extension);

        try {
            // Upload to GCS
            String gcsPath = gcsService.uploadFile("study-materials/" + sectionId, file);

            StudyMaterial material = StudyMaterial.builder()
                    .school(school)
                    .section(section)
                    .uploader(uploader)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .type(materialType)
                    .filePath(gcsPath)
                    .originalFileName(originalFilename)
                    .fileSize(file.getSize())
                    .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                    .build();

            // For videos, mark as processing and trigger async FFmpeg conversion
            if (materialType == StudyMaterialType.VIDEO) {
                material.setStatus(StudyMaterialStatus.PROCESSING);
                material = studyMaterialRepository.save(material);

                // Trigger async video processing (runs in background thread)
                videoProcessingService.processVideoAsync(material.getId(), gcsPath, sectionId);

                return mapToStudyMaterialResponse(material);
            }

            material = studyMaterialRepository.save(material);

            return mapToStudyMaterialResponse(material);

        } catch (IOException e) {
            log.error("Failed to upload file to GCS: {}", originalFilename, e);
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public StudyMaterialResponse getMaterialById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterial material = studyMaterialRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));

        // Ensure material belongs to the user's school
        if (!material.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Material not found");
        }

        // Check access for non-admin and non-teacher users
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasRole(UserRole.TEACHER) && !material.getIsPublished()) {
            throw new ForbiddenException("This material is not published");
        }

        return mapToStudyMaterialResponse(material);
    }

    @Transactional
    public StudyMaterialResponse updateMaterial(UUID id, UpdateStudyMaterialRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterial material = studyMaterialRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));

        if (request.getTitle() != null) {
            material.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            material.setDescription(request.getDescription());
        }
        if (request.getIsPublished() != null) {
            material.setIsPublished(request.getIsPublished());
        }
        if (request.getOrder() != null) {
            material.setOrder(request.getOrder());
        }

        material = studyMaterialRepository.save(material);

        return mapToStudyMaterialResponse(material);
    }

    @Transactional
    public void deleteMaterial(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterial material = studyMaterialRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));

        // Delete file from GCS
        if (material.getFilePath() != null) {
            gcsService.deleteFile(material.getFilePath());
        }

        // Delete HLS files if video
        if (material.getHlsPath() != null) {
            gcsService.deleteFile(material.getHlsPath());
        }

        // Delete thumbnail if exists
        if (material.getThumbnailPath() != null) {
            gcsService.deleteFile(material.getThumbnailPath());
        }

        studyMaterialRepository.delete(material);
    }

    @Transactional
    public StudyMaterialResponse toggleMaterialPublish(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterial material = studyMaterialRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));

        material.setIsPublished(!material.getIsPublished());
        material = studyMaterialRepository.save(material);

        return mapToStudyMaterialResponse(material);
    }

    // ============= VIDEO STREAMING =============

    @Transactional(readOnly = true)
    public StreamTokenResponse generateStreamToken(UUID materialId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UserRole userRole = SecurityUtils.getCurrentUserRole();

        // Find video material with section
        StudyMaterial material = studyMaterialRepository
                .findByIdAndSchool_IdAndTypeAndDeletedAtIsNull(materialId, schoolId, StudyMaterialType.VIDEO)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        // Check if HLS path exists
        if (material.getHlsPath() == null || material.getHlsPath().isEmpty()) {
            throw new BadRequestException("Video is not ready for streaming");
        }

        // Check if published for parents
        if (UserRole.PARENT.equals(userRole) && !material.getIsPublished()) {
            throw new ForbiddenException("This video is not published");
        }

        // Validate student enrollment for PARENT role
        if (UserRole.PARENT.equals(userRole)) {
            // Find the parent entity for this user
            Parent parent = parentRepository
                    .findByUser_IdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new ForbiddenException("Parent record not found"));

            // Find student(s) linked to this parent
            List<Student> students = studentRepository
                    .findByParent_IdAndDeletedAtIsNull(parent.getId());

            if (students.isEmpty()) {
                throw new ForbiddenException("No student record found for this parent");
            }

            // Check if any of the parent's students are enrolled in the material's class
            boolean hasAccess = students.stream()
                    .anyMatch(student -> material.getSection() != null &&
                            student.getClassEntity() != null &&
                            student.getClassEntity().getId().equals(material.getSection().getClassEntity().getId()));

            if (!hasAccess) {
                throw new ForbiddenException("Not enrolled in this class");
            }
        }

        // Generate stream token (expires in 2 hours)
        String streamToken = jwtTokenProvider.generateStreamToken(materialId, userId, schoolId);

        // Build HLS URL (GCS signed URL)
//        String streamUrl = gcsService.generateSignedUrl(material.getHlsPath());
        String streamUrl = "/api/study-materials/stream/" + materialId + "/master.m3u8";

        return StreamTokenResponse.builder()
                .streamToken(streamToken)
                .streamUrl(streamUrl)
                .material(StreamTokenResponse.MaterialInfo.builder()
                        .id(materialId)
                        .title(material.getTitle())
                        .description(material.getDescription())
                        .duration(material.getDuration())
                        .build())
                .build();
    }

    /**
     * Verify stream token for HLS streaming
     */
    public boolean verifyStreamAccess(UUID materialId, String token) {
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                return false;
            }
            // verifyStreamToken throws JWTVerificationException if invalid
            jwtTokenProvider.verifyStreamToken(token);
            return true;
        } catch (Exception e) {
            log.error("Stream token verification failed", e);
            return false;
        }
    }

    /**
     * Stream HLS file (master.m3u8, playlist.m3u8, or .ts segments) from GCS
     */
    @Transactional(readOnly = true)
    public HlsStreamResponse streamHlsFile(UUID materialId, String filename, String token) {
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedException("Missing stream token");
        }

        UUID schoolId;

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                throw new UnauthorizedException("Invalid or expired stream token");
            }

            DecodedJWT decodedJWT = jwtTokenProvider.verifyStreamToken(token);
            schoolId = UUID.fromString(decodedJWT.getClaim("schoolId").asString());

        } catch (Exception e) {
            log.error("Stream token validation failed", e);
            throw new UnauthorizedException("Invalid or expired stream token");
        }

        // Find material
        StudyMaterial material = studyMaterialRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(materialId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        if (material.getHlsPath() == null || material.getHlsPath().isEmpty()) {
            throw new ResourceNotFoundException("Video not found");
        }

        // Security: Validate filename to prevent directory traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ForbiddenException("Access denied");
        }
        String hlsDir = material.getHlsPath().substring(0, material.getHlsPath().lastIndexOf("/") + 1);
        String gcsObjectPath = hlsDir + filename;

        // Determine content type based on file extension
        String contentType;
        if (filename.endsWith(".m3u8")) {
            contentType = "application/vnd.apple.mpegurl";
        } else if (filename.endsWith(".ts")) {
            contentType = "video/mp2t";
        } else {
            throw new BadRequestException("Invalid file type");
        }

        log.info("Fetching HLS file from GCS path: {}", gcsObjectPath);

        return HlsStreamResponse.builder()
//                .signedUrl(signedUrl)
                .gcsObjectPath(gcsObjectPath)
                .contentType(contentType)
                .filename(filename)
                .build();
    }

    // ============= DOWNLOAD =============

    /**
     * Get signed URL for downloading a document (PDF/PPT/DOC)
     */
    @Transactional(readOnly = true)
    public String getDocumentDownloadUrl(UUID materialId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StudyMaterial material = studyMaterialRepository.findByIdAndDeletedAtIsNull(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));

        if (!material.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Material not found");
        }

        if (material.getType() == StudyMaterialType.VIDEO) {
            throw new BadRequestException("Use the streaming endpoint for videos");
        }

        // Check if published for non-admin users
        if (!SecurityUtils.isAdmin() && !material.getIsPublished()) {
            throw new ForbiddenException("Material not published");
        }

        // Generate signed URL for download (valid for 15 minutes)
        return gcsService.generateSignedUrl(material.getFilePath());
    }

    // ============= HELPER METHODS =============

    private StudyMaterialType detectMaterialType(String extension) {
        if (extension.matches("mp4|avi|mov|mkv|webm")) {
            return StudyMaterialType.VIDEO;
        } else if (extension.matches("pdf")) {
            return StudyMaterialType.PDF;
        } else if (extension.matches("doc|docx")) {
            return StudyMaterialType.DOCUMENT;
        } else if (extension.matches("ppt|pptx")) {
            return StudyMaterialType.PRESENTATION;
        } else {
            return StudyMaterialType.OTHER;
        }
    }

    // ============= MAPPERS =============

    private StudyMaterialSectionResponse mapToStudyMaterialSectionResponse(StudyMaterialSection section) {
        List<StudyMaterial> materials = studyMaterialRepository
                .findBySection_IdAndDeletedAtIsNull(section.getId());

        List<StudyMaterialResponse> materialResponses = materials.stream()
                .map(this::mapToStudyMaterialResponse)
                .collect(Collectors.toList());

        return StudyMaterialSectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .order(section.getOrder())
                .isPublished(section.getIsPublished())
                .classId(section.getClassEntity().getId())
                .className(section.getClassEntity().getName())
                .sectionId(section.getSection() != null ? section.getSection().getId() : null)
                .sectionName(section.getSection() != null ? section.getSection().getName() : null)
                .subjectId(section.getSubject() != null ? section.getSubject().getId() : null)
                .subjectName(section.getSubject() != null ? section.getSubject().getName() : null)
                .createdBy(section.getCreator().getId())
                .creatorName(section.getCreator().getName())
                .materials(materialResponses)
                .build();
    }

    private StudyMaterialResponse mapToStudyMaterialResponse(StudyMaterial material) {
        // Generate signed URLs for GCS files
        String fileUrl = material.getFilePath() != null ? gcsService.generateSignedUrl(material.getFilePath()) : null;

        String thumbnailUrl = material.getThumbnailPath() != null
                ? gcsService.generateSignedUrl(material.getThumbnailPath())
                : null;

        return StudyMaterialResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .description(material.getDescription())
                .type(material.getType())
                .status(material.getStatus())
                .filePath(fileUrl) // Signed URL instead of GCS path
                .hlsPath(material.getHlsPath()) // Don't expose HLS path directly
                .originalFileName(material.getOriginalFileName())
                .fileSize(material.getFileSize())
                .duration(material.getDuration())
                .isPublished(material.getIsPublished())
                .thumbnailPath(thumbnailUrl) // Signed URL for thumbnail
                .order(material.getOrder())
                .sectionId(material.getSection().getId())
                .uploadedBy(material.getUploader().getId())
                .uploaderName(material.getUploader().getName())
                .build();
    }
}