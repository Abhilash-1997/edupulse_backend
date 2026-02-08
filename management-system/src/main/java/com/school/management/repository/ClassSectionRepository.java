package com.school.management.repository;

import com.school.management.entity.ClassSection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassSectionRepository extends BaseRepository<ClassSection> {

    Optional<ClassSection> findByIdAndDeletedAtIsNull(UUID id);

    Optional<ClassSection> findByClassTeacher_IdAndDeletedAtIsNull(UUID classTeacherId);

    List<ClassSection> findByClassEntity_IdAndDeletedAtIsNull(UUID classId);

    @Query("SELECT cs FROM ClassSection cs JOIN cs.classEntity c " +
            "WHERE c.school.id = :schoolId AND cs.deletedAt IS NULL")
    List<ClassSection> findBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("SELECT cs FROM ClassSection cs JOIN cs.classEntity c " +
            "WHERE c.name = :className AND c.school.id = :schoolId AND cs.deletedAt IS NULL")
    List<ClassSection> findByClassNameAndSchoolId(
            @Param("className") String className,
            @Param("schoolId") UUID schoolId
    );
}