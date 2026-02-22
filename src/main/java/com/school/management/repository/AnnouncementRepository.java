package com.school.management.repository;

import com.school.management.entity.Announcement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends BaseRepository<Announcement> {

    Optional<Announcement> findByIdAndDeletedAtIsNull(UUID id);

    List<Announcement> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    @Query("SELECT a FROM Announcement a WHERE a.school.id = :schoolId " +
            "AND a.isActive = true " +
            "AND (a.expiryDate IS NULL OR a.expiryDate > :now) " +
            "AND a.deletedAt IS NULL " +
            "ORDER BY a.priority DESC, a.createdAt DESC")
    List<Announcement> findActiveAnnouncementsBySchool(
            @Param("schoolId") UUID schoolId,
            @Param("now") LocalDateTime now
    );

    List<Announcement> findByAuthor_IdAndTitleAndMessageAndDeletedAtIsNull(
            UUID authorId,
            String title,
            String message
    );
}