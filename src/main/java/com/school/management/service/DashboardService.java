package com.school.management.service;

import com.school.management.constant.SchoolStatus;
import com.school.management.constant.UserRole;
import com.school.management.dto.response.*;
import com.school.management.entity.School;
import com.school.management.entity.Student;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

        private final StudentRepository studentRepository;
        private final UserRepository userRepository;
        private final ClassRepository classRepository;
        private final ParentRepository parentRepository;
        private final SchoolRepository schoolRepository;
        private final SubjectRepository subjectRepository;
        private final ExamRepository examRepository;

        // ======================== SCHOOL ADMIN STATS ========================

        /**
         * Get school-level dashboard statistics.
         * Counts students, teachers, classes, parents and returns 5 most recent
         * students.
         */
        public SchoolStatsResponse getSchoolStats() {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
                log.info("Fetching school stats for schoolId: {}", schoolId);

                long studentsCount = studentRepository.countBySchool_IdAndDeletedAtIsNull(schoolId);
                long teachersCount = userRepository.countBySchool_IdAndRoleAndDeletedAtIsNull(schoolId,
                                UserRole.TEACHER);
                long classesCount = classRepository.countBySchool_IdAndDeletedAtIsNull(schoolId);
                long parentsCount = parentRepository.countBySchool_IdAndDeletedAtIsNull(schoolId);

                List<Student> recentStudents = studentRepository.findRecentBySchoolId(schoolId, PageRequest.of(0, 5));

                List<StudentResponse> recentStudentResponses = recentStudents.stream()
                                .map(this::mapToStudentResponse)
                                .collect(Collectors.toList());

                return SchoolStatsResponse.builder()
                                .totalStudents(studentsCount)
                                .totalTeachers(teachersCount)
                                .totalClasses(classesCount)
                                .totalParents(parentsCount)
                                .recentStudents(recentStudentResponses)
                                .build();
        }

        // ======================== SUPER ADMIN SYSTEM STATS ========================

        /**
         * Get system-wide statistics for Super Admin.
         * Counts schools, users, calculates mock revenue, and returns 5 recent schools.
         */
        public SystemStatsResponse getSystemStats() {
                log.info("Fetching system stats for SUPER_ADMIN");

                long schoolsCount = schoolRepository.countByDeletedAtIsNull();
                long usersCount = userRepository.countByDeletedAtIsNull();

                List<School> recentSchools = schoolRepository.findTop5ByDeletedAtIsNullOrderByCreatedAtDesc();

                List<SchoolInfoResponse> recentSchoolResponses = recentSchools.stream()
                                .map(school -> SchoolInfoResponse.builder()
                                                .id(school.getId())
                                                .name(school.getName())
                                                .createdAt(school.getCreatedAt())
                                                .status(school.getStatus())
                                                .schoolAdminEmail(
                                                                userRepository.findAdminEmailBySchoolId(school.getId())
                                                                                .orElse(null))
                                                .build())
                                .collect(Collectors.toList());

                // Mock revenue — schoolsCount * 1500 (matches Node.js behaviour)
                double totalRevenue = schoolsCount * 1500.0;

                return SystemStatsResponse.builder()
                                .totalSchools(schoolsCount)
                                .totalUsers(usersCount)
                                .totalRevenue(totalRevenue)
                                .recentSchools(recentSchoolResponses)
                                .build();
        }

        // ======================== SUPER ADMIN — GET ALL SCHOOLS
        // ========================

        /**
         * Get all schools with aggregated counts (students, teachers, staff, classes,
         * subjects, exams)
         * and the school admin email.
         */
        public List<SchoolInfoResponse> getSchools() {
                log.info("Fetching all schools with aggregated counts for SUPER_ADMIN");

                List<School> schools = schoolRepository.findAllByDeletedAtIsNullOrderByNameAsc();

                return schools.stream()
                                .map(this::mapToSchoolInfoResponse)
                                .collect(Collectors.toList());
        }

        // ======================== SUPER ADMIN — UPDATE SCHOOL STATUS
        // ========================

        /**
         * Update school status (e.g. PENDING → ACTIVE, INACTIVE).
         * Only accessible by SUPER_ADMIN.
         */
        @Transactional
        public SchoolInfoResponse updateSchoolStatus(UUID schoolId, SchoolStatus status) {
                log.info("Updating school status for schoolId: {} to {}", schoolId, status);

                School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                                .orElseThrow(() -> new com.school.management.exception.ResourceNotFoundException(
                                                "School not found"));

                school.setStatus(status);
                school = schoolRepository.save(school);

                return mapToSchoolInfoResponse(school);
        }

        // ======================== MAPPERS ========================

        private StudentResponse mapToStudentResponse(Student student) {
                StudentResponse.StudentResponseBuilder builder = StudentResponse.builder()
                                .id(student.getId())
                                .name(student.getName())
                                .admissionNumber(student.getAdmissionNumber());

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

                return builder.build();
        }

        private SchoolInfoResponse mapToSchoolInfoResponse(School school) {
                UUID schoolId = school.getId();

                return SchoolInfoResponse.builder()
                                .id(schoolId)
                                .name(school.getName())
                                .address(school.getAddress())
                                .logo(school.getLogo())
                                .board(school.getBoard())
                                .academicYear(school.getAcademicYear())
                                .status(school.getStatus())
                                .createdAt(school.getCreatedAt())
                                .schoolAdminEmail(
                                                userRepository.findAdminEmailBySchoolId(schoolId).orElse(null))
                                .studentsCount(studentRepository.countBySchool_IdAndDeletedAtIsNull(schoolId))
                                .teachersCount(userRepository.countBySchool_IdAndRoleAndDeletedAtIsNull(schoolId,
                                                UserRole.TEACHER))
                                .staffCount(userRepository.countBySchool_IdAndRoleAndDeletedAtIsNull(schoolId,
                                                UserRole.STAFF))
                                .classesCount(classRepository.countBySchool_IdAndDeletedAtIsNull(schoolId))
                                .subjectsCount(subjectRepository.countBySchool_IdAndDeletedAtIsNull(schoolId))
                                .examsCount(examRepository.countBySchool_IdAndDeletedAtIsNull(schoolId))
                                .build();
        }
}
