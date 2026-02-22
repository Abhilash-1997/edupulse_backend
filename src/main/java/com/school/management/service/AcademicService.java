package com.school.management.service;

import com.school.management.constant.DayOfWeek;
import com.school.management.constant.UserRole;
import com.school.management.dto.request.*;
import com.school.management.dto.response.*;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ConflictException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicService {

        private final ClassRepository classRepository;
        private final ClassSectionRepository classSectionRepository;
        private final SubjectRepository subjectRepository;
        private final TimetableRepository timetableRepository;
        private final UserRepository userRepository;
        private final SchoolRepository schoolRepository;
        private final StudentRepository studentRepository;

        // ============= CLASS MANAGEMENT =============

        @RequireAdmin
        @Transactional
        public ClassResponse createClass(CreateClassRequest request) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

                // Check duplicate name
                if (classRepository.findBySchool_IdAndNameAndDeletedAtIsNull(schoolId, request.getName()).isPresent()) {
                        throw new BadRequestException("Class with this name already exists");
                }

                ClassEntity classEntity = ClassEntity.builder()
                                .school(school)
                                .name(request.getName())
                                .build();

                classEntity = classRepository.save(classEntity);

                return mapToClassResponse(classEntity);
        }

        @Transactional(readOnly = true)
        public List<ClassResponse> getAllClasses() {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                List<ClassEntity> classes = classRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);

                return classes.stream()
                                .map(this::mapToClassResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<ClassResponse> getAllClassesWithCount(UUID overrideSchoolId) {
                UUID schoolId = SecurityUtils.isSuperAdmin() && overrideSchoolId != null ? overrideSchoolId
                                : SecurityUtils.getCurrentUserSchoolId();

                List<ClassEntity> classes = classRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);

                return classes.stream()
                                .map(c -> ClassResponse.builder()
                                                .id(c.getId())
                                                .name(c.getName())
                                                .studentCount(studentRepository
                                                                .countByClassEntity_IdAndDeletedAtIsNull(c.getId()))
                                                .build())
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<String> getStandards() {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
                return classRepository.findDistinctClassNamesBySchoolId(schoolId);
        }

        @Transactional(readOnly = true)
        public List<ClassSectionResponse> getDivisions(String standard) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
                List<ClassSection> sections = classSectionRepository.findByClassNameAndSchoolId(standard, schoolId);
                return sections.stream()
                                .map(this::mapToClassSectionResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<ClassSectionResponse> getAllSections(UUID overrideSchoolId) {
                UUID schoolId = SecurityUtils.isSuperAdmin() && overrideSchoolId != null ? overrideSchoolId
                                : SecurityUtils.getCurrentUserSchoolId();

                List<ClassSection> sections = classSectionRepository.findBySchoolId(schoolId);

                return sections.stream()
                                .map(this::mapToClassSectionResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public ClassSectionResponse assignClassTeacher(UUID sectionId, UUID teacherId) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                ClassSection section = classSectionRepository.findByIdAndDeletedAtIsNull(sectionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

                User teacher = userRepository.findByIdAndSchool_IdAndDeletedAtIsNull(teacherId, schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

                section.setClassTeacher(teacher);
                section = classSectionRepository.save(section);

                return mapToClassSectionResponse(section);
        }

        @Transactional(readOnly = true)
        public List<UserResponse> getTeachers(UUID overrideSchoolId) {
                UUID schoolId = SecurityUtils.isSuperAdmin() && overrideSchoolId != null ? overrideSchoolId
                                : SecurityUtils.getCurrentUserSchoolId();

                List<User> teachers = userRepository.findBySchool_IdAndRoleAndIsActiveTrueAndDeletedAtIsNull(
                                schoolId, UserRole.TEACHER);

                return teachers.stream()
                                .map(t -> UserResponse.builder()
                                                .id(t.getId())
                                                .name(t.getName())
                                                .email(t.getEmail())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Transactional
        public void deleteClass(UUID id) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                ClassEntity classEntity = classRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

                classRepository.delete(classEntity);
        }

        // ============= SECTION MANAGEMENT =============

        @Transactional
        public ClassSectionResponse createSection(CreateSectionRequest request) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                // Verify class exists
                ClassEntity classEntity = classRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                request.getClassId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

                ClassSection section = ClassSection.builder()
                                .classEntity(classEntity)
                                .name(request.getName())
                                .build();

                if (request.getClassTeacherId() != null) {
                        User teacher = userRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                        request.getClassTeacherId(), schoolId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
                        section.setClassTeacher(teacher);
                }

                section = classSectionRepository.save(section);

                return mapToClassSectionResponse(section);
        }

        @Transactional(readOnly = true)
        public List<ClassSectionResponse> getSectionsByClass(UUID classId) {
                List<ClassSection> sections = classSectionRepository.findByClassEntity_IdAndDeletedAtIsNull(classId);

                return sections.stream()
                                .map(this::mapToClassSectionResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void deleteSection(UUID id) {
                ClassSection section = classSectionRepository.findByIdAndDeletedAtIsNull(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

                classSectionRepository.delete(section);
        }

        // ============= SUBJECT MANAGEMENT =============

        @Transactional
        public SubjectResponse createSubject(CreateSubjectRequest request) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

                ClassEntity classEntity = classRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                request.getClassId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

                // Check duplicate code
                if (request.getCode() != null &&
                                subjectRepository.findBySchool_IdAndCodeAndDeletedAtIsNull(schoolId, request.getCode())
                                                .isPresent()) {
                        throw new BadRequestException("Subject code already exists");
                }

                Subject subject = Subject.builder()
                                .school(school)
                                .classEntity(classEntity)
                                .name(request.getName())
                                .code(request.getCode())
                                .build();

                subject = subjectRepository.save(subject);

                return mapToSubjectResponse(subject);
        }

        @Transactional(readOnly = true)
        public List<SubjectResponse> getAllSubjects(UUID classId) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                List<Subject> subjects;

                if (classId != null) {
                        subjects = subjectRepository.findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(schoolId,
                                        classId);
                } else {
                        subjects = subjectRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
                }

                return subjects.stream()
                                .map(this::mapToSubjectResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void deleteSubject(UUID id) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                Subject subject = subjectRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

                subjectRepository.delete(subject);
        }

        // ============= TIMETABLE MANAGEMENT =============

        @Transactional
        public TimetableResponse createTimetableEntry(CreateTimetableRequest request) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

                // Validate section, subject, teacher
                ClassSection section = classSectionRepository.findByIdAndDeletedAtIsNull(request.getSectionId())
                                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

                Subject subject = subjectRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                request.getSubjectId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

                User teacher = userRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                request.getTeacherId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
                // Parse times
                LocalTime startTime = LocalTime.parse(request.getStartTime());
                LocalTime endTime = LocalTime.parse(request.getEndTime());

                // Check for conflicts
                List<Timetable> conflicts = timetableRepository.findConflictingTimetables(
                                List.of(teacher.getId()),
                                request.getDayOfWeek(),
                                startTime,
                                endTime);

                if (!conflicts.isEmpty()) {
                        throw new BadRequestException("Teacher already has a class during this time");
                }

                Timetable timetable = Timetable.builder()
                                .school(school)
                                .section(section)
                                .subject(subject)
                                .teacher(teacher)
                                .dayOfWeek(request.getDayOfWeek())
                                .startTime(startTime)
                                .endTime(endTime)
                                .classroom(request.getRoom())
                                .build();

                timetable = timetableRepository.save(timetable);

                return mapToTimetableResponse(timetable);
        }

        @Transactional
        public List<TimetableResponse> createDailyTimetable(CreateDailyTimetableRequest request) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                // Validate section exists
                ClassSection section = classSectionRepository.findByIdAndDeletedAtIsNull(request.getSectionId())
                                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

                List<CreateDailyTimetableRequest.TimetablePeriod> periods = request.getPeriods();

                // === Conflict Detection ===

                // 1. Check for Internal Conflicts (within the payload itself)
                for (int i = 0; i < periods.size(); i++) {
                        for (int j = i + 1; j < periods.size(); j++) {
                                CreateDailyTimetableRequest.TimetablePeriod p1 = periods.get(i);
                                CreateDailyTimetableRequest.TimetablePeriod p2 = periods.get(j);

                                if (p1.getTeacherId() != null && p1.getTeacherId().equals(p2.getTeacherId())) {
                                        LocalTime start1 = LocalTime.parse(p1.getStartTime());
                                        LocalTime end1 = LocalTime.parse(p1.getEndTime());
                                        LocalTime start2 = LocalTime.parse(p2.getStartTime());
                                        LocalTime end2 = LocalTime.parse(p2.getEndTime());

                                        // Overlap: (StartA < EndB) and (EndA > StartB)
                                        if (start1.isBefore(end2) && end1.isAfter(start2)) {
                                                throw new ConflictException(String.format(
                                                                "Internal Conflict: Teacher is assigned to multiple overlapping periods (Times: %s-%s and %s-%s)",
                                                                p1.getStartTime(), p1.getEndTime(), p2.getStartTime(),
                                                                p2.getEndTime()));
                                        }
                                }
                        }
                }

                // 2. Database Conflict Check (Against other sections)
                List<UUID> teacherIds = periods.stream()
                                .map(CreateDailyTimetableRequest.TimetablePeriod::getTeacherId)
                                .filter(id -> id != null)
                                .distinct()
                                .collect(Collectors.toList());

                if (!teacherIds.isEmpty()) {
                        for (CreateDailyTimetableRequest.TimetablePeriod period : periods) {
                                if (period.getTeacherId() == null)
                                        continue;

                                LocalTime startTime = LocalTime.parse(period.getStartTime());
                                LocalTime endTime = LocalTime.parse(period.getEndTime());

                                // Find conflicts excluding current section (since we're replacing its
                                // schedule)
                                List<Timetable> conflicts = timetableRepository
                                                .findConflictingTimetablesExcludingSection(
                                                                List.of(period.getTeacherId()),
                                                                request.getDayOfWeek(),
                                                                startTime,
                                                                endTime,
                                                                request.getSectionId());

                                if (!conflicts.isEmpty()) {
                                        Timetable conflict = conflicts.get(0);
                                        String teacherName = conflict.getTeacher() != null
                                                        ? conflict.getTeacher().getName()
                                                        : "Teacher";
                                        String className = conflict.getSection() != null
                                                        ? conflict.getSection().getClassEntity().getName() + "-"
                                                                        + conflict.getSection().getName()
                                                        : "another class";
                                        throw new ConflictException(String.format(
                                                        "Conflict: %s is already assigned to %s from %s to %s",
                                                        teacherName, className,
                                                        conflict.getStartTime().toString().substring(0, 5),
                                                        conflict.getEndTime().toString().substring(0, 5)));
                                }
                        }
                }

                // === All conflict checks passed, now delete old and create new ===

                // Delete existing timetable for this section and day
                timetableRepository.deleteBySection_IdAndDayOfWeekAndDeletedAtIsNull(
                                request.getSectionId(),
                                request.getDayOfWeek());

                School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

                List<TimetableResponse> responses = new ArrayList<>();

                for (CreateDailyTimetableRequest.TimetablePeriod period : periods) {
                        Subject subject = subjectRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                        period.getSubjectId(), schoolId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

                        User teacher = userRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                        period.getTeacherId(), schoolId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

                        LocalTime startTime = LocalTime.parse(period.getStartTime());
                        LocalTime endTime = LocalTime.parse(period.getEndTime());

                        Timetable timetable = Timetable.builder()
                                        .school(school)
                                        .section(section)
                                        .subject(subject)
                                        .teacher(teacher)
                                        .dayOfWeek(request.getDayOfWeek())
                                        .startTime(startTime)
                                        .endTime(endTime)
                                        .classroom(period.getClassroom())
                                        .build();

                        timetable = timetableRepository.save(timetable);
                        responses.add(mapToTimetableResponse(timetable));
                }

                return responses;
        }

        @Transactional(readOnly = true)
        public List<TimetableResponse> getTimetableBySection(UUID sectionId) {
                List<Timetable> timetables = timetableRepository.findBySection_IdAndDeletedAtIsNull(sectionId);

                return timetables.stream()
                                .map(this::mapToTimetableResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void deleteTimetableEntry(UUID id) {
                Timetable timetable = timetableRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found"));

                timetableRepository.delete(timetable);
        }

        // ============= MAPPERS =============

        private ClassResponse mapToClassResponse(ClassEntity classEntity) {
                return ClassResponse.builder()
                                .id(classEntity.getId())
                                .name(classEntity.getName())
                                .build();
        }

        private ClassSectionResponse mapToClassSectionResponse(ClassSection section) {
                return ClassSectionResponse.builder()
                                .id(section.getId())
                                .name(section.getName())
                                .classId(section.getClassEntity().getId())
                                .className(section.getClassEntity().getName())
                                .classTeacherId(section.getClassTeacher() != null ? section.getClassTeacher().getId()
                                                : null)
                                .classTeacherName(
                                                section.getClassTeacher() != null ? section.getClassTeacher().getName()
                                                                : null)
                                .build();
        }

        private SubjectResponse mapToSubjectResponse(Subject subject) {
                return SubjectResponse.builder()
                                .id(subject.getId())
                                .name(subject.getName())
                                .code(subject.getCode())
                                .classId(subject.getClassEntity().getId())
                                .classInfo(ClassResponse.builder()
                                                .id(subject.getClassEntity().getId())
                                                .name(subject.getClassEntity().getName())
                                                .build())
                                .build();
        }

        private TimetableResponse mapToTimetableResponse(Timetable timetable) {
                return TimetableResponse.builder()
                                .id(timetable.getId())
                                .dayOfWeek(timetable.getDayOfWeek())
                                .startTime(timetable.getStartTime())
                                .endTime(timetable.getEndTime())
                                .classroom(timetable.getClassroom())
                                .subject(mapToSubjectResponse(timetable.getSubject()))
                                .teacher(timetable.getTeacher() != null ? mapToUserResponse(timetable.getTeacher())
                                                : null)
                                .section(timetable.getSection() != null
                                                ? mapToClassSectionResponse(timetable.getSection())
                                                : null)

                                .build();
        }

        private UserResponse mapToUserResponse(User user) {
                return UserResponse.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .build();
        }
}