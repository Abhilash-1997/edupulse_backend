package com.school.management.repository;

import com.school.management.entity.StudyMaterialSection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyMaterialSectionRepository extends BaseRepository<StudyMaterialSection> {

    Optional<StudyMaterialSection> findByIdAndDeletedAtIsNull(UUID id);

    Optional<StudyMaterialSection> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    List<StudyMaterialSection> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<StudyMaterialSection> findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(UUID schoolId, UUID classId);

    @Query("SELECT sms FROM StudyMaterialSection sms WHERE sms.school.id = :schoolId " +
            "AND (:classId IS NULL OR sms.classEntity.id = :classId) " +
            "AND (:subjectId IS NULL OR sms.subject.id = :subjectId) " +
            "AND (:sectionId IS NULL OR sms.section.id = :sectionId) " +
            "AND sms.deletedAt IS NULL " +
            "ORDER BY sms.order ASC, sms.createdAt DESC")
    List<StudyMaterialSection> findByFilters(
            @Param("schoolId") UUID schoolId,
            @Param("classId") UUID classId,
            @Param("subjectId") UUID subjectId,
            @Param("sectionId") UUID sectionId
    );
}