package com.school.management.repository;

import com.school.management.constant.StaffStatus;
import com.school.management.entity.StaffProfile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffProfileRepository extends BaseRepository<StaffProfile> {

    Optional<StaffProfile> findByUser_IdAndDeletedAtIsNull(UUID userId);
    Optional<StaffProfile> findByIdAndDeletedAtIsNull(UUID id);
    Optional<StaffProfile> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);
    boolean existsBySchool_IdAndEmployeeCodeAndDeletedAtIsNull(UUID schoolId, String employeeCode);
    List<StaffProfile> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);
    List<StaffProfile> findBySchool_IdAndStatusAndDeletedAtIsNull(UUID schoolId, StaffStatus status);

    @Query("SELECT sp FROM StaffProfile sp WHERE sp.school.id = :schoolId " +
            "AND sp.status = 'ACTIVE' AND sp.joiningDate <= :date AND sp.deletedAt IS NULL")
    List<StaffProfile> findActiveStaffBySchoolAndJoinedBeforeDate(
            @Param("schoolId") UUID schoolId,
            @Param("date") LocalDate date
    );

    @Query("SELECT sp FROM StaffProfile sp WHERE sp.school.id = :schoolId " +
            "AND sp.status = 'ACTIVE' AND sp.id IN :staffIds AND sp.deletedAt IS NULL")
    List<StaffProfile> findActiveStaffBySchoolAndIds(
            @Param("schoolId") UUID schoolId,
            @Param("staffIds") List<UUID> staffIds
    );
}