package com.school.management.repository;

import com.school.management.entity.StaffAttendance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffAttendanceRepository extends BaseRepository<StaffAttendance> {

    Optional<StaffAttendance> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<StaffAttendance> findBySchool_IdAndStaff_IdAndDateAndDeletedAtIsNull(
            UUID schoolId,
            UUID staffId,
            LocalDate date
    );

    List<StaffAttendance> findBySchool_IdAndDateAndDeletedAtIsNull(UUID schoolId, LocalDate date);

    @Query("SELECT sa FROM StaffAttendance sa WHERE sa.staff.id = :staffId " +
            "AND sa.date BETWEEN :startDate AND :endDate AND sa.deletedAt IS NULL")
    List<StaffAttendance> findByStaffIdAndDateRange(
            @Param("staffId") UUID staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}