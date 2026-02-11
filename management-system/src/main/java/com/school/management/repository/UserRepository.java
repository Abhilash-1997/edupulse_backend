package com.school.management.repository;

import com.school.management.constant.UserRole;
import com.school.management.entity.School;
import com.school.management.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends BaseRepository<User> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    Optional<User> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT u FROM User u WHERE u.school.id = :schoolId AND u.id != :currentUserId " +
            "AND u.deletedAt IS NULL ORDER BY u.name ASC")
    List<User> findChatUsersExcludingCurrent(
            @Param("schoolId") UUID schoolId,
            @Param("currentUserId") UUID currentUserId
    );

    @Query("SELECT u FROM User u WHERE u.school.id = :schoolId AND u.id != :currentUserId " +
            "AND u.name LIKE %:search% AND u.deletedAt IS NULL ORDER BY u.name ASC")
    List<User> findChatUsersExcludingCurrentWithSearch(
            @Param("schoolId") UUID schoolId,
            @Param("currentUserId") UUID currentUserId,
            @Param("search") String search
    );

    List<User> findBySchool_IdAndRoleAndIsActiveTrueAndDeletedAtIsNull(UUID schoolId, UserRole role);
}