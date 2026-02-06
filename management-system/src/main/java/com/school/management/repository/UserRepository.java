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

    Optional<User> findByIdAndSchoolIdAndDeletedAtIsNull(UUID id, UUID schoolId);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}