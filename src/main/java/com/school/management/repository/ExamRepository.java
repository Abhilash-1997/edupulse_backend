package com.school.management.repository;

import com.school.management.entity.Exam;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamRepository extends BaseRepository<Exam> {

    Optional<Exam> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Exam> findBySchool_IdAndNameAndDeletedAtIsNull(UUID schoolId, String name);

    List<Exam> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<Exam> findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(UUID schoolId, UUID classId);

    @Query("SELECT e FROM Exam e WHERE e.school.id = :schoolId AND e.classEntity.id = :classId " +
            "AND (e.section IS NULL OR e.section.id = :sectionId) AND e.deletedAt IS NULL " +
            "ORDER BY e.startDate DESC")
    List<Exam> findBySchoolAndClassAndSection(
            @Param("schoolId") UUID schoolId,
            @Param("classId") UUID classId,
            @Param("sectionId") UUID sectionId
    );

    @Query("SELECT e FROM Exam e WHERE e.school.id = :schoolId AND e.classEntity.id = :classId " +
            "AND e.section IS NULL AND e.deletedAt IS NULL ORDER BY e.startDate DESC")
    List<Exam> findBySchoolAndClassOnly(
            @Param("schoolId") UUID schoolId,
            @Param("classId") UUID classId
    );

    long countBySchool_IdAndDeletedAtIsNull(UUID schoolId);
}