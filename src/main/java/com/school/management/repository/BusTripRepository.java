package com.school.management.repository;

import com.school.management.constant.BusTripStatus;
import com.school.management.entity.BusTrip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusTripRepository extends BaseRepository<BusTrip> {

    Optional<BusTrip> findByIdAndDeletedAtIsNull(UUID id);

    Optional<BusTrip> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<BusTrip> findByBus_IdAndStatusAndDeletedAtIsNull(UUID busId, BusTripStatus status);

    List<BusTrip> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<BusTrip> findBySchool_IdAndBus_IdAndDeletedAtIsNull(UUID schoolId, UUID busId);

    @Query("SELECT bt FROM BusTrip bt WHERE bt.school.id = :schoolId " +
            "AND (:busId IS NULL OR bt.bus.id = :busId) " +
            "AND (:status IS NULL OR bt.status = :status) " +
            "AND (:startDate IS NULL OR bt.startTime BETWEEN :startDate AND :endDate) " +
            "AND bt.deletedAt IS NULL " +
            "ORDER BY bt.startTime DESC")
    List<BusTrip> findByFilters(
            @Param("schoolId") UUID schoolId,
            @Param("busId") UUID busId,
            @Param("status") BusTripStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}