package com.school.management.repository;

import com.school.management.entity.Student;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends BaseRepository<Student> {

    Optional<Student> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Student> findByIdAndSchool_IdAndDeletedAtIsNull(
            UUID id,
            UUID schoolId
    );

    Optional<Student> findBySchool_IdAndAdmissionNumberAndDeletedAtIsNull(
            UUID schoolId,
            String admissionNumber
    );

    boolean existsBySchool_IdAndAdmissionNumberAndDeletedAtIsNull(UUID schoolId, String admissionNumber);
    List<Student> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);
    List<Student> findBySchool_IdAndParent_IdAndDeletedAtIsNull(UUID schoolId, UUID parentId);
    List<Student> findBySchool_IdAndParent_IdIsNullAndDeletedAtIsNull(UUID schoolId);
    List<Student> findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(UUID schoolId, UUID classId);

    List<Student> findBySection_IdAndSchool_IdAndDeletedAtIsNull(UUID sectionId, UUID schoolId);

    List<Student> findByParent_IdAndDeletedAtIsNull(UUID parentId);

    long countBySchool_IdAndDeletedAtIsNull(UUID schoolId);   //for count

    @Query("""
        SELECT s
        FROM Student s
        WHERE s.school.id = :schoolId
          AND s.deletedAt IS NULL
        ORDER BY s.createdAt DESC
    """)
    List<Student> findTop5RecentBySchoolId(@Param("schoolId") UUID schoolId);


    //FOR id CARD GENERATION of students
    @Query("""
        SELECT s FROM Student s
        LEFT JOIN FETCH s.school
        LEFT JOIN FETCH s.parent p
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH s.classEntity
        LEFT JOIN FETCH s.section
        WHERE s.id = :id 
          AND s.school.id = :schoolId 
          AND s.deletedAt IS NULL
    """)
    Optional<Student> findByIdAndSchoolIdWithDetails(
            @Param("id") UUID id,
            @Param("schoolId") UUID schoolId
    );
}
