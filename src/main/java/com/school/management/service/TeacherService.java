package com.school.management.service;

import com.school.management.constant.UserRole;
import com.school.management.dto.response.*;
import com.school.management.entity.ClassSection;
import com.school.management.entity.Parent;
import com.school.management.entity.Student;
import com.school.management.entity.Timetable;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

        private final ClassSectionRepository classSectionRepository;
        private final StudentRepository studentRepository;
        private final TimetableRepository timetableRepository;
        private final ParentRepository parentRepository;

        // ======================== GET MY CLASS ========================

        /**
         * Get the class (section) assigned to the current teacher as Class Teacher,
         * including class info and enrolled students.
         */
        public MyClassResponse getMyClass() {
                UUID teacherId = SecurityUtils.getCurrentUserId();
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
                log.info("Teacher {} fetching assigned class for school {}", teacherId, schoolId);

                List<ClassSection> sections = classSectionRepository
                                .findByClassTeacher_IdAndDeletedAtIsNull(teacherId);

                if (sections.isEmpty()) {
                        throw new ResourceNotFoundException("No class assigned to you as Class Teacher");
                }

                // Find section belonging to teacher's school
                ClassSection section = sections.stream()
                                .filter(s -> s.getClassEntity() != null
                                                && s.getClassEntity().getSchool() != null
                                                && s.getClassEntity().getSchool().getId().equals(schoolId))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "No class assigned to you as a Class Teacher"));

                List<Student> students = studentRepository
                                .findBySection_IdAndSchool_IdAndDeletedAtIsNull(section.getId(), schoolId);

                return MyClassResponse.builder()
                                .section(mapToSectionResponse(section))
                                .classInfo(ClassResponse.builder()
                                                .id(section.getClassEntity().getId())
                                                .name(section.getClassEntity().getName())
                                                .build())
                                .students(students.stream()
                                                .map(this::mapToStudentResponse)
                                                .collect(Collectors.toList()))
                                .build();
        }

        // ======================== GET MY STUDENTS ========================

        /**
         * Get students of the section assigned to the current teacher,
         * sorted alphabetically by name.
         */
        public List<StudentResponse> getMyStudents() {
                UUID teacherId = SecurityUtils.getCurrentUserId();
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
                log.info("Teacher {} fetching students for school {}", teacherId, schoolId);

                List<ClassSection> sections = classSectionRepository
                                .findByClassTeacher_IdAndDeletedAtIsNull(teacherId);

                ClassSection section = sections.stream()
                                .filter(s -> s.getClassEntity() != null
                                                && s.getClassEntity().getSchool() != null
                                                && s.getClassEntity().getSchool().getId().equals(schoolId))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "You are not assigned as a Class Teacher to any class."));

                List<Student> students = studentRepository
                                .findBySection_IdAndSchool_IdAndDeletedAtIsNullOrderByNameAsc(
                                                section.getId(), schoolId);

                return students.stream()
                                .map(this::mapToStudentResponse)
                                .collect(Collectors.toList());
        }

        // ======================== GET ID CARD DATA ========================

        /**
         * Generate ID card data for a student.
         * Access control:
         * - TEACHER: must be the class teacher of the student's section
         * - PARENT: must own the student
         * - SCHOOL_ADMIN / SUPER_ADMIN: unrestricted within school
         */
        public IDCardDataResponse getIDCardData(UUID studentId) {
                UUID userId = SecurityUtils.getCurrentUserId();
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
                UserRole role = SecurityUtils.getCurrentUserRole();
                log.info("User {} (role={}) requesting ID card for student {}", userId, role, studentId);

                Student student = studentRepository
                                .findByIdAndSchoolIdWithDetails(studentId, schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

                // Access control
                if (role == UserRole.TEACHER) {
                        if (student.getSection() == null
                                        || student.getSection().getClassTeacher() == null
                                        || !student.getSection().getClassTeacher().getId().equals(userId)) {
                                throw new AccessDeniedException(
                                                "You can only generate ID cards for your own class.");
                        }
                } else if (role == UserRole.PARENT) {
                        Parent parent = parentRepository.findByUser_IdAndDeletedAtIsNull(userId)
                                        .orElseThrow(() -> new AccessDeniedException(
                                                        "Unauthorized to access this student's ID card"));
                        if (student.getParent() == null
                                        || !student.getParent().getId().equals(parent.getId())) {
                                throw new AccessDeniedException(
                                                "Unauthorized to access this student's ID card");
                        }
                }
                // SCHOOL_ADMIN and SUPER_ADMIN pass through

                String classInfo = "N/A";
                if (student.getClassEntity() != null && student.getSection() != null) {
                        classInfo = student.getClassEntity().getName() + " - " + student.getSection().getName();
                } else if (student.getClassEntity() != null) {
                        classInfo = student.getClassEntity().getName();
                }

                String fatherName = "Parent";
                if (student.getParent() != null && student.getParent().getUser() != null) {
                        fatherName = student.getParent().getUser().getName();
                }

                return IDCardDataResponse.builder()
                                .schoolName(student.getSchool().getName())
                                .schoolAddress(student.getSchool().getAddress())
                                .schoolLogo(student.getSchool().getLogo())
                                .studentName(student.getName())
                                .admissionNumber(student.getAdmissionNumber())
                                .classInfo(classInfo)
                                .dob(student.getDob() != null ? student.getDob().toString() : "N/A")
                                .bloodGroup("N/A")
                                .fatherName(fatherName)
                                .emergencyContact("N/A")
                                .build();
        }

        // ======================== GET MY PERIODS ========================

        /**
         * Get all timetable periods assigned to the current teacher.
         */
        public List<TimetableResponse> getMyPeriods() {
                UUID teacherId = SecurityUtils.getCurrentUserId();
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
                log.info("Teacher {} fetching periods for school {}", teacherId, schoolId);

                List<Timetable> periods = timetableRepository
                                .findByTeacher_IdAndSchool_IdAndDeletedAtIsNull(teacherId, schoolId);

                return periods.stream()
                                .map(this::mapToTimetableResponse)
                                .collect(Collectors.toList());
        }

        // ======================== GET MY CLASS TIMETABLE ========================

        /**
         * Get the full timetable for the section managed by the current teacher
         * as Class Teacher.
         */
        public MyClassTimetableResponse getMyClassTimetable() {
                UUID teacherId = SecurityUtils.getCurrentUserId();
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
                log.info("Teacher {} fetching class timetable for school {}", teacherId, schoolId);

                List<ClassSection> sections = classSectionRepository
                                .findByClassTeacher_IdAndDeletedAtIsNull(teacherId);

                ClassSection section = sections.stream()
                                .filter(s -> s.getClassEntity() != null
                                                && s.getClassEntity().getSchool() != null
                                                && s.getClassEntity().getSchool().getId().equals(schoolId))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "You are not assigned as a Class Teacher to any class."));

                List<Timetable> timetableEntries = timetableRepository
                                .findBySection_IdAndDeletedAtIsNull(section.getId());

                return MyClassTimetableResponse.builder()
                                .classDetails(mapToSectionResponse(section))
                                .timetable(timetableEntries.stream()
                                                .map(this::mapToTimetableResponse)
                                                .collect(Collectors.toList()))
                                .build();
        }

        // ======================== MAPPERS ========================

        private ClassSectionResponse mapToSectionResponse(ClassSection section) {
                ClassSectionResponse.ClassSectionResponseBuilder builder = ClassSectionResponse.builder()
                                .id(section.getId())
                                .name(section.getName());

                if (section.getClassEntity() != null) {
                        builder.classId(section.getClassEntity().getId())
                                        .className(section.getClassEntity().getName());
                }

                if (section.getClassTeacher() != null) {
                        builder.classTeacherId(section.getClassTeacher().getId())
                                        .classTeacherName(section.getClassTeacher().getName());
                }

                return builder.build();
        }

        private StudentResponse mapToStudentResponse(Student student) {
                StudentResponse.StudentResponseBuilder builder = StudentResponse.builder()
                                .id(student.getId())
                                .name(student.getName())
                                .admissionNumber(student.getAdmissionNumber())
                                .dob(student.getDob())
                                .gender(student.getGender())
                                .profilePicture(student.getProfilePicture());

                if (student.getClassEntity() != null) {
                        builder.classId(student.getClassEntity().getId())
                                        .classInfo(ClassResponse.builder()
                                                        .id(student.getClassEntity().getId())
                                                        .name(student.getClassEntity().getName())
                                                        .build());
                }

                if (student.getSection() != null) {
                        builder.sectionId(student.getSection().getId())
                                        .sectionInfo(ClassSectionResponse.builder()
                                                        .id(student.getSection().getId())
                                                        .name(student.getSection().getName())
                                                        .build());
                }

                if (student.getParent() != null) {
                        builder.parentId(student.getParent().getId());
                }

                return builder.build();
        }

        private TimetableResponse mapToTimetableResponse(Timetable timetable) {
                TimetableResponse.TimetableResponseBuilder builder = TimetableResponse.builder()
                                .id(timetable.getId())
                                .dayOfWeek(timetable.getDayOfWeek())
                                .startTime(timetable.getStartTime())
                                .endTime(timetable.getEndTime())
                                .classroom(timetable.getClassroom());

                if (timetable.getSubject() != null) {
                        builder.subject(SubjectResponse.builder()
                                        .id(timetable.getSubject().getId())
                                        .name(timetable.getSubject().getName())
                                        .code(timetable.getSubject().getCode())
                                        .build());
                }

                if (timetable.getTeacher() != null) {
                        builder.teacher(UserResponse.builder()
                                        .id(timetable.getTeacher().getId())
                                        .name(timetable.getTeacher().getName())
                                        .build());
                }

                if (timetable.getSection() != null) {
                        builder.section(ClassSectionResponse.builder()
                                        .id(timetable.getSection().getId())
                                        .name(timetable.getSection().getName())
                                        .classId(timetable.getSection().getClassEntity() != null
                                                        ? timetable.getSection().getClassEntity().getId()
                                                        : null)
                                        .className(timetable.getSection().getClassEntity() != null
                                                        ? timetable.getSection().getClassEntity().getName()
                                                        : null)
                                        .build());
                }

                return builder.build();
        }
}
