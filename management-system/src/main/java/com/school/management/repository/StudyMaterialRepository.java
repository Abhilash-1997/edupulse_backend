package com.school.management.repository;

import com.school.management.constant.StudyMaterialType;
import com.school.management.entity.StudyMaterial;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyMaterialRepository extends BaseRepository<StudyMaterial> {

    Optional<StudyMaterial> findByIdAndDeletedAtIsNull(UUID id);

    Optional<StudyMaterial> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<StudyMaterial> findByIdAndSchool_IdAndTypeAndDeletedAtIsNull(
            UUID id,
            UUID schoolId,
            StudyMaterialType type
    );

    List<StudyMaterial> findBySection_IdAndDeletedAtIsNull(UUID sectionId);

    List<StudyMaterial> findBySection_IdAndIsPublishedTrueAndDeletedAtIsNull(UUID sectionId);
}