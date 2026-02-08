package com.school.management.repository;

import com.school.management.constant.MessageStatus;
import com.school.management.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends BaseRepository<Message> {

    Optional<Message> findByIdAndReceiver_IdAndDeletedAtIsNull(UUID id, UUID receiverId);

    List<Message> findByConversation_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID conversationId,
            Pageable pageable
    );

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.createdAt < :cursor AND m.deletedAt IS NULL " +
            "ORDER BY m.createdAt DESC")
    List<Message> findByConversationIdBeforeCursor(
            @Param("conversationId") UUID conversationId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable
    );

    long countByConversation_IdAndReceiver_IdAndStatusNotAndDeletedAtIsNull(
            UUID conversationId,
            UUID receiverId,
            MessageStatus status
    );

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findLatestByConversationId(@Param("conversationId") UUID conversationId);
}