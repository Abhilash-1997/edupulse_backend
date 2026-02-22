package com.school.management.repository;

import com.school.management.entity.Conversation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends BaseRepository<Conversation> {

    Optional<Conversation> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT c FROM Conversation c WHERE c.school.id = :schoolId " +
            "AND (c.userA.id = :userId OR c.userB.id = :userId) " +
            "AND c.deletedAt IS NULL " +
            "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findBySchoolIdAndUserId(
            @Param("schoolId") UUID schoolId,
            @Param("userId") UUID userId
    );

    @Query("SELECT c FROM Conversation c WHERE c.school.id = :schoolId " +
            "AND ((c.userA.id = :userAId AND c.userB.id = :userBId) " +
            "OR (c.userA.id = :userBId AND c.userB.id = :userAId)) " +
            "AND c.deletedAt IS NULL")
    Optional<Conversation> findBySchoolIdAndBothUsers(
            @Param("schoolId") UUID schoolId,
            @Param("userAId") UUID userAId,
            @Param("userBId") UUID userBId
    );
}