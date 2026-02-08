package com.school.management.repository;

import com.school.management.entity.LibrarySection;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LibrarySectionRepository extends BaseRepository<LibrarySection> {

    Optional<LibrarySection> findByIdAndDeletedAtIsNull(UUID id);

    Optional<LibrarySection> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<LibrarySection> findBySchool_IdAndNameAndDeletedAtIsNull(UUID schoolId, String name);

    List<LibrarySection> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    long countBySchool_IdAndDeletedAtIsNull(UUID schoolId);
}