package com.school.management.repository;

import com.school.management.entity.Bus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusRepository extends BaseRepository<Bus> {

    Optional<Bus> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Bus> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<Bus> findByDriver_IdAndSchool_IdAndDeletedAtIsNull(UUID driverId, UUID schoolId);

    Optional<Bus> findBySchool_IdAndBusNumberAndDeletedAtIsNull(UUID schoolId, String busNumber);

    Optional<Bus> findBySchool_IdAndRegistrationNumberAndDeletedAtIsNull(
            UUID schoolId,
            String registrationNumber
    );

    List<Bus> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<Bus> findBySchool_IdAndIsActiveAndDeletedAtIsNull(UUID schoolId, Boolean isActive);
}