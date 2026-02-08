package com.school.management.repository;

import com.school.management.entity.Subject;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends BaseRepository<Subject> {

    Optional<Subject> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Subject> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<Subject> findBySchool_IdAndCodeAndDeletedAtIsNull(UUID schoolId, String code);

    List<Subject> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<Subject> findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(UUID schoolId, UUID classId);

    long countBySchool_IdAndDeletedAtIsNull(UUID schoolId);
}