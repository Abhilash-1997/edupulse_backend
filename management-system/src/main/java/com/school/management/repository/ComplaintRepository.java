package com.school.management.repository;

import com.school.management.constant.ComplaintPriority;
import com.school.management.constant.ComplaintStatus;
import com.school.management.entity.Complaint;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends BaseRepository<Complaint> {

    Optional<Complaint> findByIdAndDeletedAtIsNull(UUID id);

    List<Complaint> findByParent_IdAndDeletedAtIsNull(UUID parentId);

    List<Complaint> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<Complaint> findBySchool_IdAndStatusAndDeletedAtIsNull(UUID schoolId, ComplaintStatus status);

    List<Complaint> findBySchool_IdAndPriorityAndDeletedAtIsNull(UUID schoolId, ComplaintPriority priority);

    List<Complaint> findBySchool_IdAndStatusAndPriorityAndDeletedAtIsNull(
            UUID schoolId,
            ComplaintStatus status,
            ComplaintPriority priority
    );
}