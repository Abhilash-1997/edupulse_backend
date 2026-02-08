package com.school.management.repository;

import com.school.management.entity.ExamResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamResultRepository extends BaseRepository<ExamResult> {

    Optional<ExamResult> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<ExamResult> findByExam_IdAndStudent_IdAndSubject_IdAndDeletedAtIsNull(
            UUID examId,
            UUID studentId,
            UUID subjectId
    );

    List<ExamResult> findByExam_IdAndDeletedAtIsNull(UUID examId);

    List<ExamResult> findByExam_IdAndSubject_IdAndDeletedAtIsNull(UUID examId, UUID subjectId);

    List<ExamResult> findByStudent_IdAndDeletedAtIsNull(UUID studentId);

    List<ExamResult> findByStudent_IdAndExam_IdAndDeletedAtIsNull(UUID studentId, UUID examId);

    List<ExamResult> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    @Query("SELECT er FROM ExamResult er WHERE er.school.id = :schoolId " +
            "AND (:examId IS NULL OR er.exam.id = :examId) " +
            "AND (:subjectId IS NULL OR er.subject.id = :subjectId) " +
            "AND (:classId IS NULL OR er.student.classEntity.id = :classId) " +
            "AND (:sectionId IS NULL OR er.student.section.id = :sectionId) " +
            "AND er.deletedAt IS NULL")
    List<ExamResult> findByFilters(
            @Param("schoolId") UUID schoolId,
            @Param("examId") UUID examId,
            @Param("subjectId") UUID subjectId,
            @Param("classId") UUID classId,
            @Param("sectionId") UUID sectionId
    );

    @Query("SELECT SUM(er.marksObtained) FROM ExamResult er " +
            "WHERE er.exam.id = :examId AND er.student.id = :studentId AND er.deletedAt IS NULL")
    Float sumMarksByExamAndStudent(@Param("examId") UUID examId, @Param("studentId") UUID studentId);
}