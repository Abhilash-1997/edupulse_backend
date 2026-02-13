package com.school.management.service;

import com.school.management.dto.request.CreateGalleryRequest;
import com.school.management.dto.response.GalleryImageResponse;
import com.school.management.dto.response.GalleryResponse;
import com.school.management.entity.Gallery;
import com.school.management.entity.GalleryImage;
import com.school.management.entity.School;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.GalleryImageRepository;
import com.school.management.repository.GalleryRepository;
import com.school.management.repository.SchoolRepository;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final SchoolRepository schoolRepository;
    private final GcsService gcsService;

    @Transactional
    public GalleryResponse createGallery(CreateGalleryRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        Gallery gallery = Gallery.builder()
                .school(school)
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .build();

        gallery = galleryRepository.save(gallery);

        return mapToGalleryResponse(gallery);
    }

    /**
     * Get all galleries
     */
    @Transactional(readOnly = true)
    public List<GalleryResponse> getAllGalleries() {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Gallery> galleries = galleryRepository.findBySchool_IdAndDeletedAtIsNullOrderByEventDateDesc(schoolId);

        return galleries.stream()
                .map(this::mapToGalleryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get gallery by ID
     */
    @Transactional(readOnly = true)
    public GalleryResponse getGalleryById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Gallery gallery = galleryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));

        if (!gallery.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Gallery not found");
        }

        return mapToGalleryResponse(gallery);
    }

    /**
     * Upload images to gallery
     */
    @Transactional
    public GalleryResponse uploadImages(UUID galleryId, List<MultipartFile> images) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Gallery gallery = galleryRepository.findByIdAndDeletedAtIsNull(galleryId)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));

        if (!gallery.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Gallery not found");
        }

        if (images == null || images.isEmpty()) {
            throw new BadRequestException("No images provided");
        }

        for (MultipartFile image : images) {
            try {
                String objectName = gcsService.uploadFile("galleries/" + galleryId, image);

                GalleryImage galleryImage = GalleryImage.builder().gallery(gallery).imageUrl(objectName).build();
                galleryImageRepository.save(galleryImage);
            } catch (IOException e) {
                log.error("Failed to upload image: {}", image.getOriginalFilename(), e);
                throw new BadRequestException("Failed to upload image: " + e.getMessage());
            }
        }


        return mapToGalleryResponse(gallery);
    }

    /**
     * Update gallery
     */
    @Transactional
    public GalleryResponse updateGallery(UUID id, CreateGalleryRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Gallery gallery = galleryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));

        if (!gallery.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Gallery not found");
        }

        if (request.getTitle() != null) {
            gallery.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            gallery.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            gallery.setEventDate(request.getEventDate());
        }

        gallery = galleryRepository.save(gallery);

        return mapToGalleryResponse(gallery);
    }

    /**
     * Delete gallery
     */
    @Transactional
    public void deleteGallery(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Gallery gallery = galleryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));

        if (!gallery.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Gallery not found");
        }

        List<GalleryImage> images = galleryImageRepository.findByGallery_IdAndDeletedAtIsNull(id);
        for (GalleryImage image : images) {
            gcsService.deleteFile(image.getImageUrl());
        }

        galleryRepository.delete(gallery);
    }

    /**
     * Delete single image from gallery
     */
    @Transactional
    public void deleteImage(UUID galleryId, UUID imageId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Gallery gallery = galleryRepository.findByIdAndDeletedAtIsNull(galleryId)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery not found"));

        if (!gallery.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Gallery not found");
        }
        GalleryImage image = galleryImageRepository.findById(imageId).orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (!image.getGallery().getId().equals(galleryId)) {
            throw new ResourceNotFoundException("Image not found in this gallery");
        }
        gcsService.deleteFile(image.getImageUrl());
        galleryImageRepository.delete(image);
    }

    private GalleryResponse mapToGalleryResponse(Gallery gallery) {
        List<GalleryImage> images = galleryImageRepository.findByGallery_IdAndDeletedAtIsNull(gallery.getId());

        List<GalleryImageResponse> imageResponses = images.stream()
                .map(img -> GalleryImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(gcsService.generateSignedUrl(img.getImageUrl()))
                        .galleryId(img.getGallery().getId())
                        .build())
                .collect(Collectors.toList());

        return GalleryResponse.builder()
                .id(gallery.getId())
                .title(gallery.getTitle())
                .description(gallery.getDescription())
                .eventDate(gallery.getEventDate())
                .images(imageResponses)
                .build();
    }
}