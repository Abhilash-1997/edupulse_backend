package com.school.management.service;

import com.school.management.dto.request.BulkUpdateStudentsRequest;
import com.school.management.dto.request.CreateStudentRequest;
import com.school.management.dto.request.UpdateStudentRequest;
import com.school.management.dto.response.StudentResponse;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ClassRepository classRepository;
    private final ClassSectionRepository classSectionRepository;
    private final String uploadDir = "uploads/students";

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        // Check duplicate admission number
        if (studentRepository.existsBySchool_IdAndAdmissionNumberAndDeletedAtIsNull(
                schoolId, request.getAdmissionNumber())) {
            throw new BadRequestException("Admission number already exists");
        }

        School school = new School();
        school.setId(schoolId);

        Student student = Student.builder()
                .school(school)
                .name(request.getName())
                .admissionNumber(request.getAdmissionNumber())
                .dob(request.getDob())
                .gender(request.getGender())
                .build();

        // Set parent if provided
        if (request.getParentId() != null) {
            Parent parent = new Parent();
            parent.setId(request.getParentId());
            student.setParent(parent);
        }

        // Set class if provided
        if (request.getClassId() != null) {
            ClassEntity classEntity = new ClassEntity();
            classEntity.setId(request.getClassId());
            student.setClassEntity(classEntity);
        }

        // Set section if provided
        if (request.getSectionId() != null) {
            ClassSection section = new ClassSection();
            section.setId(request.getSectionId());
            student.setSection(section);
        }

        student = studentRepository.save(student);

        return mapToStudentResponse(student);
    }

    @Transactional
    public StudentResponse updateStudent(UUID id, UpdateStudentRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Update fields
        if (request.getName() != null) {
            student.setName(request.getName());
        }
        if (request.getDob() != null) {
            student.setDob(request.getDob());
        }
        if (request.getGender() != null) {
            student.setGender(request.getGender());
        }
        if (request.getParentId() != null) {
            Parent parent = new Parent();
            parent.setId(request.getParentId());
            student.setParent(parent);
        }
        if (request.getClassId() != null) {
            ClassEntity classEntity = new ClassEntity();
            classEntity.setId(request.getClassId());
            student.setClassEntity(classEntity);
        }
        if (request.getSectionId() != null) {
            ClassSection section = new ClassSection();
            section.setId(request.getSectionId());
            student.setSection(section);
        }

        student = studentRepository.save(student);

        return mapToStudentResponse(student);
    }

    @Transactional
    public void bulkUpdateStudents(BulkUpdateStudentsRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Student> students = studentRepository.findAllById(request.getStudentIds());

        for (Student student : students) {
            if (!student.getSchool().getId().equals(schoolId)) {
                continue; // Skip students from other schools
            }

            if (request.getSectionId() != null) {
                ClassSection section = new ClassSection();
                section.setId(request.getSectionId());
                student.setSection(section);
            }

            if (request.getParentId() != null) {
                Parent parent = new Parent();
                parent.setId(request.getParentId());
                student.setParent(parent);
            }
        }

        studentRepository.saveAll(students);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents(UUID parentId, UUID classId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Student> students;

        if (parentId != null) {
            if ("null".equals(parentId.toString())) {
                students = studentRepository.findBySchool_IdAndParent_IdIsNullAndDeletedAtIsNull(schoolId);
            } else {
                students = studentRepository.findBySchool_IdAndParent_IdAndDeletedAtIsNull(schoolId, parentId);
            }
        } else if (classId != null) {
            students = studentRepository.findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(schoolId, classId);
        } else {
            students = studentRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        }

        return students.stream()
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        return mapToStudentResponse(student);
    }

    @Transactional
    public String uploadProfilePicture(UUID id, MultipartFile file) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Delete old profile picture if exists
        if (student.getProfilePicture() != null) {
            try {
                Path oldFile = Paths.get(student.getProfilePicture());
                Files.deleteIfExists(oldFile);
            } catch (IOException e) {
                log.error("Failed to delete old profile picture", e);
            }
        }

        // Save new file
        try {
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = uploadDir + "/" + filename;
            student.setProfilePicture(relativePath);
            studentRepository.save(student);

            return relativePath;

        } catch (IOException e) {
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteStudent(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        studentRepository.delete(student); // Soft delete
    }

    private StudentResponse mapToStudentResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .admissionNumber(student.getAdmissionNumber())
                .dob(student.getDob())
                .gender(student.getGender())
                .profilePicture(student.getProfilePicture())
                .parentId(student.getParent() != null ? student.getParent().getId() : null)
                .classId(student.getClassEntity() != null ? student.getClassEntity().getId() : null)
                .sectionId(student.getSection() != null ? student.getSection().getId() : null)
                .build();
    }
}