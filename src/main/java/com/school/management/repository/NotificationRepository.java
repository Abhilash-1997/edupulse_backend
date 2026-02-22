package com.school.management.repository;

import com.school.management.entity.Notification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends BaseRepository<Notification> {

    Optional<Notification> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);

    List<Notification> findTop50ByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
}