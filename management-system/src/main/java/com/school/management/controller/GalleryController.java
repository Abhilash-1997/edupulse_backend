package com.school.management.controller;

import com.school.management.dto.request.CreateGalleryRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.GalleryResponse;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.service.GalleryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Gallery Management Controller
 * Base path: /gallery
 */
@Slf4j
@RestController
@RequestMapping("/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;

    // ======================== CREATE ========================

    /**
     * Create a new gallery (Admin only)
     * POST /gallery
     */
    @PostMapping("/")
    @RequireAdmin
    public ResponseEntity<ApiResponse<GalleryResponse>> createGallery(
            @Valid @RequestBody CreateGalleryRequest request) {

        log.info("Creating gallery: {}", request.getTitle());

        GalleryResponse response = galleryService.createGallery(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gallery created successfully", response));
    }

    // ======================== READ ========================

    /**
     * Get all galleries
     * GET /gallery
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<GalleryResponse>>> getAllGalleries() {
        log.info("Fetching all galleries");
        List<GalleryResponse> response = galleryService.getAllGalleries();
        return ResponseEntity.ok(
                ApiResponse.successWithCount("Galleries retrieved successfully", response, response.size()));
    }

    /**
     * Get gallery by ID
     * GET /gallery/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GalleryResponse>> getGalleryById(@PathVariable UUID id) {

        log.info("Fetching gallery by ID: {}", id);

        GalleryResponse response = galleryService.getGalleryById(id);

        return ResponseEntity.ok(ApiResponse.success("Gallery retrieved successfully", response));
    }

    // ======================== UPDATE ========================

    /**
     * Update gallery details (Admin only)
     * PUT /gallery/{id}
     */
    @PutMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<GalleryResponse>> updateGallery(
            @PathVariable UUID id,
            @Valid @RequestBody CreateGalleryRequest request) {

        log.info("Updating gallery: {}", id);

        GalleryResponse response = galleryService.updateGallery(id, request);

        return ResponseEntity.ok(ApiResponse.success("Gallery updated successfully", response));
    }

    // ======================== IMAGE UPLOAD ========================

    /**
     * Upload images to a gallery (Admin only)
     * POST /gallery/{galleryId}/images
     */
    @PostMapping(value = "/{galleryId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireAdmin
    public ResponseEntity<ApiResponse<GalleryResponse>> uploadImages(
            @PathVariable UUID galleryId,
            @RequestParam("images") List<MultipartFile> images) {

        log.info("Uploading {} images to gallery: {}", images.size(), galleryId);
        GalleryResponse response = galleryService.uploadImages(galleryId, images);
        return ResponseEntity.ok(ApiResponse.success("Images uploaded successfully", response));
    }

    // ======================== DELETE ========================

    /**
     * Delete a gallery (Admin only)
     * DELETE /gallery/{id}
     */
    @DeleteMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> deleteGallery(@PathVariable UUID id) {
        log.info("Deleting gallery: {}", id);
        galleryService.deleteGallery(id);
        return ResponseEntity.ok(ApiResponse.success("Gallery deleted successfully", null));
    }

    /**
     * Delete a single image from a gallery (Admin only)
     * DELETE /gallery/{galleryId}/images/{imageId}
     */
    @DeleteMapping("/{galleryId}/images/{imageId}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID galleryId,
            @PathVariable UUID imageId) {

        log.info("Deleting image {} from gallery: {}", imageId, galleryId);
        galleryService.deleteImage(galleryId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }
}
