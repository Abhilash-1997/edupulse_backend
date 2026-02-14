package com.school.management.controller;

import com.school.management.dto.request.CreateStudyMaterialSectionRequest;
import com.school.management.dto.request.UpdateStudyMaterialRequest;
import com.school.management.dto.request.UpdateStudyMaterialSectionRequest;
import com.school.management.dto.request.UploadStudyMaterialRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.HlsStreamResponse;
import com.school.management.dto.response.StreamTokenResponse;
import com.school.management.dto.response.StudyMaterialResponse;
import com.school.management.dto.response.StudyMaterialSectionResponse;
import com.school.management.service.StudyMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Study Material Management Controller
 * Base path: /study-materials
 */
@Slf4j
@RestController
@RequestMapping("/study-materials")
@RequiredArgsConstructor
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;

    // ======================== STREAMING (Public with token auth)
    // ========================

    /**
     * Stream HLS files (token-based auth, no JWT header needed)
     * GET /study-materials/hls/{filename}
     * Returns a redirect to the GCS signed URL for the HLS file.
     */
    @GetMapping("/hls/{filename}")
    public ResponseEntity<Void> streamHLS(
            @PathVariable String filename,
            @RequestParam String token) {
        log.info("Streaming HLS file: {}", filename);
        HlsStreamResponse response = studyMaterialService.streamHlsFile(filename, token);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(response.getSignedUrl()))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .build();
    }

    // ======================== SECTION MANAGEMENT ========================

    /**
     * Create a new section (Teacher, Admin)
     * POST /study-materials/sections
     */
    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialSectionResponse>> createSection(
            @Valid @RequestBody CreateStudyMaterialSectionRequest request) {
        log.info("Creating study material section: {}", request.getTitle());
        StudyMaterialSectionResponse response = studyMaterialService.createSection(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Section created successfully", response));
    }

    /**
     * Get all sections (filter by class/subject/section)
     * GET /study-materials/sections?classId=...&subjectId=...&sectionId=...
     */
    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<List<StudyMaterialSectionResponse>>> getAllSections(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID sectionId) {
        log.info("Fetching sections, classId: {}, subjectId: {}, sectionId: {}", classId, subjectId, sectionId);
        List<StudyMaterialSectionResponse> response = studyMaterialService.getSections(classId, subjectId, sectionId);
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Sections retrieved successfully", response, response.size()));
    }

    /**
     * Get section by ID with materials
     * GET /study-materials/sections/{id}
     */
    @GetMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<StudyMaterialSectionResponse>> getSectionById(@PathVariable UUID id) {
        log.info("Fetching section by ID: {}", id);
        StudyMaterialSectionResponse response = studyMaterialService.getSectionById(id);
        return ResponseEntity.ok(ApiResponse.success("Section retrieved successfully", response));
    }

    /**
     * Update section (Teacher, Admin)
     * PUT /study-materials/sections/{id}
     */
    @PutMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialSectionResponse>> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudyMaterialSectionRequest request) {
        log.info("Updating section: {}", id);
        StudyMaterialSectionResponse response = studyMaterialService.updateSection(id, request);
        return ResponseEntity.ok(ApiResponse.success("Section updated successfully", response));
    }

    /**
     * Delete section (Teacher, Admin)
     * DELETE /study-materials/sections/{id}
     */
    @DeleteMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID id) {
        log.info("Deleting section: {}", id);
        studyMaterialService.deleteSection(id);
        return ResponseEntity.ok(ApiResponse.success("Section deleted successfully", null));
    }

    /**
     * Toggle section publish status (Teacher, Admin)
     * PATCH /study-materials/sections/{id}/publish
     */
    @PatchMapping("/sections/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialSectionResponse>> toggleSectionPublish(@PathVariable UUID id) {
        log.info("Toggling publish status for section: {}", id);
        StudyMaterialSectionResponse response = studyMaterialService.toggleSectionPublish(id);
        return ResponseEntity.ok(ApiResponse.success("Section publish status toggled successfully", response));
    }

    // ======================== MATERIAL MANAGEMENT ========================

    /**
     * Upload material to section (Teacher, Admin)
     * POST /study-materials/sections/{sectionId}/materials
     */
    @PostMapping(value = "/sections/{sectionId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> uploadMaterial(
            @PathVariable UUID sectionId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isPublished", required = false) Boolean isPublished,
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading material to section: {}", sectionId);

        UploadStudyMaterialRequest request = UploadStudyMaterialRequest.builder()
                .title(title)
                .description(description)
                .isPublished(isPublished)
                .sectionId(sectionId)
                .build();

        StudyMaterialResponse response = studyMaterialService.uploadMaterial(sectionId, request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Material uploaded successfully", response));
    }

    /**
     * Get material by ID
     * GET /study-materials/materials/{id}
     */
    @GetMapping("/materials/{id}")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> getMaterialById(@PathVariable UUID id) {
        log.info("Fetching material by ID: {}", id);
        StudyMaterialResponse response = studyMaterialService.getMaterialById(id);
        return ResponseEntity.ok(ApiResponse.success("Material retrieved successfully", response));
    }

    /**
     * Update material (Teacher, Admin)
     * PUT /study-materials/materials/{id}
     */
    @PutMapping("/materials/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> updateMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudyMaterialRequest request) {
        log.info("Updating material: {}", id);
        StudyMaterialResponse response = studyMaterialService.updateMaterial(id, request);
        return ResponseEntity.ok(ApiResponse.success("Material updated successfully", response));
    }

    /**
     * Delete material (Teacher, Admin)
     * DELETE /study-materials/materials/{id}
     */
    @DeleteMapping("/materials/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(@PathVariable UUID id) {
        log.info("Deleting material: {}", id);
        studyMaterialService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.success("Material deleted successfully", null));
    }

    /**
     * Toggle material publish status (Teacher, Admin)
     * PATCH /study-materials/materials/{id}/publish
     */
    @PatchMapping("/materials/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterialResponse>> toggleMaterialPublish(@PathVariable UUID id) {
        log.info("Toggling publish status for material: {}", id);
        StudyMaterialResponse response = studyMaterialService.toggleMaterialPublish(id);
        return ResponseEntity.ok(ApiResponse.success("Material publish status toggled successfully", response));
    }

    // ======================== STREAMING ========================

    /**
     * Get stream token for video (requires auth)
     * GET /study-materials/materials/{id}/stream
     */
    @GetMapping("/materials/{id}/stream")
    public ResponseEntity<ApiResponse<StreamTokenResponse>> getStreamToken(@PathVariable UUID id) {
        log.info("Generating stream token for material: {}", id);
        StreamTokenResponse response = studyMaterialService.generateStreamToken(id);
        return ResponseEntity.ok(ApiResponse.success("Stream token generated successfully", response));
    }

    /**
     * Download document (PDF/PPT/DOC)
     * GET /study-materials/materials/{id}/download
     * Redirects to GCS signed URL for download.
     */
    @GetMapping("/materials/{id}/download")
    public ResponseEntity<Void> downloadDocument(@PathVariable UUID id) {
        log.info("Downloading material: {}", id);
        String signedUrl = studyMaterialService.getDocumentDownloadUrl(id);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(signedUrl))
                .build();
    }
}
