package com.school.management.repository;

import com.school.management.entity.BusRoute;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusRouteRepository extends BaseRepository<BusRoute> {

    Optional<BusRoute> findByIdAndDeletedAtIsNull(UUID id);

    Optional<BusRoute> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    List<BusRoute> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<BusRoute> findBySchool_IdAndBus_IdAndDeletedAtIsNull(UUID schoolId, UUID busId);

    List<BusRoute> findByBus_IdAndIsActiveAndDeletedAtIsNull(UUID busId, Boolean isActive);
}