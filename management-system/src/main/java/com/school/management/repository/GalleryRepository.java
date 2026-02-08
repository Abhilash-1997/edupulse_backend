package com.school.management.repository;

import com.school.management.entity.Gallery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GalleryRepository extends BaseRepository<Gallery> {

    Optional<Gallery> findByIdAndDeletedAtIsNull(UUID id);

    List<Gallery> findBySchool_IdAndDeletedAtIsNullOrderByEventDateDesc(UUID schoolId);
}