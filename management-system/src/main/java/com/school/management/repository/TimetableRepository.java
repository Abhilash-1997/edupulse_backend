package com.school.management.repository;

import com.school.management.constant.DayOfWeek;
import com.school.management.entity.Timetable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableRepository extends BaseRepository<Timetable> {

    List<Timetable> findBySection_IdAndDeletedAtIsNull(UUID sectionId);

    List<Timetable> findByTeacher_IdAndDeletedAtIsNull(UUID teacherId);

    List<Timetable> findByTeacher_IdAndSchool_IdAndDeletedAtIsNull(UUID teacherId, UUID schoolId);

    List<Timetable> findBySection_IdAndDayOfWeekAndDeletedAtIsNull(UUID sectionId, DayOfWeek dayOfWeek);

    void deleteBySection_IdAndDayOfWeekAndDeletedAtIsNull(UUID sectionId, DayOfWeek dayOfWeek);

    @Query("SELECT t FROM Timetable t WHERE t.teacher.id IN :teacherIds " +
            "AND t.dayOfWeek = :dayOfWeek " +
            "AND ((t.startTime < :endTime AND t.endTime > :startTime)) " +
            "AND t.deletedAt IS NULL")
    List<Timetable> findConflictingTimetables(
            @Param("teacherIds") List<UUID> teacherIds,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}