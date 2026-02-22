package com.school.management.repository;

import com.school.management.entity.Attendance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends BaseRepository<Attendance> {

    Optional<Attendance> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<Attendance> findBySchool_IdAndStudent_IdAndDateAndDeletedAtIsNull(
            UUID schoolId,
            UUID studentId,
            LocalDate date
    );

    List<Attendance> findBySchool_IdAndSection_IdAndDateAndDeletedAtIsNull(
            UUID schoolId,
            UUID sectionId,
            LocalDate date
    );

    List<Attendance> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<Attendance> findBySchool_IdAndSection_IdAndDeletedAtIsNull(UUID schoolId, UUID sectionId);

    List<Attendance> findBySchool_IdAndDateAndDeletedAtIsNull(UUID schoolId, LocalDate date);

    List<Attendance> findByStudent_IdAndDeletedAtIsNull(UUID studentId);

    @Query("SELECT a FROM Attendance a WHERE a.school.id = :schoolId " +
            "AND a.date BETWEEN :startDate AND :endDate AND a.deletedAt IS NULL")
    List<Attendance> findBySchoolIdAndDateRange(
            @Param("schoolId") UUID schoolId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}