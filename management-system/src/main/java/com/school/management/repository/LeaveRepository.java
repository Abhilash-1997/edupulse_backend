package com.school.management.repository;

import com.school.management.constant.LeaveStatus;
import com.school.management.entity.Leave;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveRepository extends BaseRepository<Leave> {

    Optional<Leave> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    List<Leave> findByApplicant_IdAndDeletedAtIsNull(UUID applicantId);

    List<Leave> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<Leave> findBySchool_IdAndStatusAndDeletedAtIsNull(UUID schoolId, LeaveStatus status);

    List<Leave> findBySchool_IdAndRoleAndDeletedAtIsNull(UUID schoolId, String role);

    List<Leave> findBySchool_IdAndStatusAndRoleAndDeletedAtIsNull(
            UUID schoolId,
            LeaveStatus status,
            String role
    );
}