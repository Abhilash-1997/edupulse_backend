package com.school.management.repository;

import com.school.management.entity.Payroll;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollRepository extends BaseRepository<Payroll> {

    Optional<Payroll> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Payroll> findBySchool_IdAndStaff_IdAndMonthAndYearAndDeletedAtIsNull(
            UUID schoolId,
            UUID staffId,
            String month,
            Integer year
    );

    List<Payroll> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<Payroll> findBySchool_IdAndMonthAndDeletedAtIsNull(UUID schoolId, String month);

    List<Payroll> findBySchool_IdAndYearAndDeletedAtIsNull(UUID schoolId, Integer year);

    List<Payroll> findBySchool_IdAndMonthAndYearAndDeletedAtIsNull(
            UUID schoolId,
            String month,
            Integer year
    );

    List<Payroll> findBySchool_IdAndStaff_IdAndDeletedAtIsNull(UUID schoolId, UUID staffId);
}