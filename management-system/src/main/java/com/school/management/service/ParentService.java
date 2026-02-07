package com.school.management.service;

import com.school.management.constant.UserRole;
import com.school.management.dto.request.CreateParentRequest;
import com.school.management.dto.response.MyChildrenResponse;
import com.school.management.dto.response.ParentResponse;
import com.school.management.dto.response.StudentResponse;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.exception.UnauthorizedException;
import com.school.management.repository.ParentRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.repository.UserRepository;
import com.school.management.util.PasswordUtil;
import com.school.management.security.annotation.RequireParent;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordUtil passwordUtil;
    //private final EmailService emailService;

    //================================== CREATE PARENT ===============================================

    @Transactional
    public ParentResponse createParent(CreateParentRequest request) {
        System.out.println("Request studentIds:----------------> " + request.getStudentIds());
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserRole currentRole = SecurityUtils.getCurrentUserRole();

        if (currentRole == UserRole.TEACHER) {
            List<Student> students = studentRepository.findAllById(request.getStudentIds());
            for (Student student : students) {
                ClassSection section = student.getSection();

                if (section == null) {
                    throw new BadRequestException("Student " + student.getName() + " is not assigned to any section");
                }

                User classTeacher = section.getClassTeacher();
                if (classTeacher == null || !classTeacher.getId().equals(currentUserId)) {
                    throw new UnauthorizedException("You can only create parents for students in your own sections. "
                            +"Student: " + student.getName() + " is in section: " + section.getName());
                }
            }
        }

        // Check if email exists
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        School school = new School();
        school.setId(schoolId);

        // Create User
        User user = User.builder()
                .school(school)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordUtil.hashPassword(request.getPassword()))
                .role(UserRole.PARENT)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        // Create Parent
        Parent parent = Parent.builder()
                .school(school)
                .user(user)
                .guardianName(request.getName())
                .occupation(request.getOccupation())
                .build();
        parent = parentRepository.save(parent);

        // Link students
        List<Student> students = studentRepository.findAllById(request.getStudentIds());
        List<String> studentNames = students.stream()
                .map(Student::getName)
                .toList();

        for (Student student : students) {
            student.setParent(parent);
        }
        studentRepository.saveAll(students);

        // Send email with raw password
//        try {
//            emailService.sendParentAccountEmail(
//                    school.getName(),
//                    user.getName(),
//                    user.getEmail(),
//                    request.getPassword(),
//                    studentNames
//            );
//        } catch (Exception e) {
//            log.error("Failed to send parent account email", e);
//        }

        return mapToParentResponse(parent);
    }

    //================================== GET ALL PARENTS ===============================================

    @Transactional(readOnly = true)
    public List<ParentResponse> getAllParents() {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Parent> parents = parentRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);

        return parents.stream()
                .map(this::mapToParentResponse)
                .collect(Collectors.toList());
    }

    //================================== GET MY CHILDREN BASED ON PARENT ID ===============================================

    @RequireParent
    @Transactional(readOnly = true)
    public MyChildrenResponse getMyChildren() {
        UUID userId = SecurityUtils.getCurrentUserId();

        Parent parent = parentRepository.findByUser_IdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found"));

        List<Student> students = studentRepository.findByParent_IdAndDeletedAtIsNull(parent.getId());

        List<MyChildrenResponse.ChildInfoResponse> children = students.stream()
                .map(student -> {
                    // Fee summary calculation (to be implemented)
                    MyChildrenResponse.FeeSummary feeSummary = MyChildrenResponse.FeeSummary.builder()
                            .totalFees(0.0f)
                            .paidFees(0.0f)
                            .pendingFees(0.0f)
                            .build();

                    Float attendanceRate = 0.0f;

                    // Latest exam average (has bug - uses obtainedMarks/totalMarks)
                    Float latestExamAverage = 0.0f;

                    return MyChildrenResponse.ChildInfoResponse.builder()
                            .student(mapToStudentResponse(student))
                            .feeSummary(feeSummary)
                            .attendanceRate(attendanceRate)
                            .latestExamAverage(latestExamAverage)
                            .build();
                })
                .collect(Collectors.toList());

        return MyChildrenResponse.builder()
                .children(children)
                .build();
    }

    private ParentResponse mapToParentResponse(Parent parent) {
        return ParentResponse.builder()
                .id(parent.getId())
                .guardianName(parent.getGuardianName())
                .occupation(parent.getOccupation())
                .build();
    }

    private StudentResponse mapToStudentResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .admissionNumber(student.getAdmissionNumber())
                .dob(student.getDob())
                .gender(student.getGender())
                .build();
    }
}