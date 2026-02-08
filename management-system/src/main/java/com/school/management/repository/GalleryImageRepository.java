package com.school.management.repository;

import com.school.management.entity.GalleryImage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GalleryImageRepository extends BaseRepository<GalleryImage> {

    List<GalleryImage> findByGallery_IdAndDeletedAtIsNull(UUID galleryId);

    void deleteByGallery_Id(UUID galleryId);
}