package com.school.management.repository;

import com.school.management.entity.StudentBusAssignment;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentBusAssignmentRepository extends BaseRepository<StudentBusAssignment> {

    Optional<StudentBusAssignment> findByStudent_IdAndDeletedAtIsNull(UUID studentId);

    Optional<StudentBusAssignment> findByStudent_IdAndSchool_IdAndDeletedAtIsNull(
            UUID studentId,
            UUID schoolId
    );

    List<StudentBusAssignment> findBySchool_IdAndIsActiveTrueAndDeletedAtIsNull(UUID schoolId);

    List<StudentBusAssignment> findBySchool_IdAndBus_IdAndIsActiveTrueAndDeletedAtIsNull(
            UUID schoolId,
            UUID busId
    );
}