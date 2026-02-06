package com.school.management.repository;

import com.school.management.constant.SchoolStatus;
import com.school.management.entity.School;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends BaseRepository<School> {
    Optional<School> findByIdAndDeletedAtIsNull(UUID id);

}