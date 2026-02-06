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

}