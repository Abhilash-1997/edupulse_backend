package com.school.management.repository;

import com.school.management.entity.Parent;
import com.school.management.entity.User;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends BaseRepository<Parent> {
    Optional<Parent> findByUser_IdAndDeletedAtIsNull(UUID userId);
    Optional<Parent> findByIdAndDeletedAtIsNull(UUID id);
    List<Parent> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);
}