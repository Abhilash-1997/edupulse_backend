package com.school.management.repository;

import com.school.management.entity.PasswordResetToken;
import com.school.management.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends BaseRepository<PasswordResetToken> {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    void deleteByUser(User user);
}
