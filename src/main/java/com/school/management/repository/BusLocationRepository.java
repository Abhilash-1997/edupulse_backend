package com.school.management.repository;

import com.school.management.entity.BusLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusLocationRepository extends JpaRepository<BusLocation, UUID> {

    Optional<BusLocation> findFirstByBus_IdOrderByTimestampDesc(UUID busId);

    List<BusLocation> findByTrip_IdOrderByTimestampAsc(UUID tripId);

    List<BusLocation> findByBus_IdOrderByTimestampDesc(UUID busId);
}