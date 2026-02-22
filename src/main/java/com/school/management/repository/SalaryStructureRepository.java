package com.school.management.repository;

import com.school.management.entity.SalaryStructure;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryStructureRepository extends BaseRepository<SalaryStructure> {

    Optional<SalaryStructure> findByStaff_IdAndDeletedAtIsNull(UUID staffId);

    Optional<SalaryStructure> findByIdAndDeletedAtIsNull(UUID id);
}