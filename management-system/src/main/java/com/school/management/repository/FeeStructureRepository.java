package com.school.management.repository;

import com.school.management.entity.FeeStructure;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeStructureRepository extends BaseRepository<FeeStructure> {

    Optional<FeeStructure> findByIdAndDeletedAtIsNull(UUID id);

    Optional<FeeStructure> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    List<FeeStructure> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<FeeStructure> findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(UUID schoolId, UUID classId);
}