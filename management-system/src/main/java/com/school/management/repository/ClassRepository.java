package com.school.management.repository;

import com.school.management.entity.ClassEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassRepository extends BaseRepository<ClassEntity> {

    Optional<ClassEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<ClassEntity> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<ClassEntity> findBySchool_IdAndNameAndDeletedAtIsNull(UUID schoolId, String name);

    List<ClassEntity> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    long countBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    @Query("SELECT DISTINCT c.name FROM ClassEntity c WHERE c.school.id = :schoolId " +
            "AND c.deletedAt IS NULL ORDER BY c.name ASC")
    List<String> findDistinctClassNamesBySchoolId(@Param("schoolId") UUID schoolId);
}