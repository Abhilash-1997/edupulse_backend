package com.school.management.repository;

import com.school.management.constant.LibraryTransactionStatus;
import com.school.management.entity.LibraryTransaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LibraryTransactionRepository extends BaseRepository<LibraryTransaction> {

    Optional<LibraryTransaction> findByIdAndDeletedAtIsNull(UUID id);

    List<LibraryTransaction> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<LibraryTransaction> findBySchool_IdAndUser_IdAndDeletedAtIsNull(UUID schoolId, UUID userId);

    List<LibraryTransaction> findBySchool_IdAndStatusAndDeletedAtIsNull(
            UUID schoolId,
            LibraryTransactionStatus status
    );

    long countBySchool_IdAndStatusAndDeletedAtIsNull(UUID schoolId, LibraryTransactionStatus status);

    @Query("SELECT COUNT(lt) FROM LibraryTransaction lt WHERE lt.school.id = :schoolId " +
            "AND lt.status = 'ISSUED' AND lt.dueDate < :now AND lt.deletedAt IS NULL")
    long countOverdueBooks(@Param("schoolId") UUID schoolId, @Param("now") LocalDateTime now);
}