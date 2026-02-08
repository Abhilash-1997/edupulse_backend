package com.school.management.repository;

import com.school.management.entity.GradeRule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradeRuleRepository extends BaseRepository<GradeRule> {

    List<GradeRule> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    @Query("SELECT gr FROM GradeRule gr WHERE gr.school.id = :schoolId " +
            "AND :percentage >= gr.minPercentage AND :percentage <= gr.maxPercentage " +
            "AND gr.deletedAt IS NULL")
    Optional<GradeRule> findBySchoolIdAndPercentage(
            @Param("schoolId") UUID schoolId,
            @Param("percentage") Float percentage
    );
}