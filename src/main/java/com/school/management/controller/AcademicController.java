package com.school.management.controller;

import com.school.management.constant.DayOfWeek;
import com.school.management.dto.request.*;
import com.school.management.dto.response.*;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.security.annotation.RequireAdminOrTeacher;
import com.school.management.security.annotation.RequireTeacher;
import com.school.management.service.AcademicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/academics")
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academicService;

    // ============= CLASS MANAGEMENT ENDPOINTS =============

    @PostMapping("/classes")
    @RequireAdmin
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(@Valid @RequestBody CreateClassRequest request) {
        ClassResponse response = academicService.createClass(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Class created Successfully", response));
    }

    @GetMapping("/classes")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<List<ClassResponse>>> getAllClasses() {
        List<ClassResponse> classes = academicService.getAllClasses();
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @GetMapping("/classes/with-count")
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<ClassResponse>>> getAllClassesWithCount(
            @RequestParam(required = false) UUID schoolId) {
        List<ClassResponse> classes = academicService.getAllClassesWithCount(schoolId);
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @GetMapping("/classes/standards")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<List<String>>> getStandards() {
        List<String> standards = academicService.getStandards();
        return ResponseEntity.ok(ApiResponse.success(standards));
    }

    @DeleteMapping("/classes/{id}")
    @RequireAdmin
    public ResponseEntity<Void> deleteClass(
            @PathVariable UUID id) {
        academicService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }

    // ============= SECTION MANAGEMENT ENDPOINTS =============

    @PostMapping("/sections")
    @RequireAdmin
    public ResponseEntity<ApiResponse<ClassSectionResponse>> createSection(@Valid @RequestBody CreateSectionRequest request) {
        ClassSectionResponse response = academicService.createSection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                                                            "Section created Successfully",
                                                            response));
    }

    @GetMapping("/sections")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<List<ClassSectionResponse>>> getAllSections(
            @RequestParam(required = false) UUID schoolId) {
        List<ClassSectionResponse> sections = academicService.getAllSections(schoolId);
        return ResponseEntity.ok(ApiResponse.success(sections));
    }

    @GetMapping("/classes/standards/{standard}/divisions")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<List<ClassSectionResponse>>> getSectionsByClass(
            @PathVariable UUID standard) {
        List<ClassSectionResponse> sections = academicService.getSectionsByClass(standard);
        return ResponseEntity.ok(ApiResponse.success(sections));
    }

    @GetMapping("/divisions")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<List<ClassSectionResponse>>> getDivisions(
            @RequestParam String standard) {
        List<ClassSectionResponse> divisions = academicService.getDivisions(standard);
        return ResponseEntity.ok(ApiResponse.success(divisions));
    }

    @PutMapping("/sections/{sectionId}/class-teacher/{teacherId}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<ClassSectionResponse>> assignClassTeacher(
            @PathVariable UUID sectionId,
            @PathVariable UUID teacherId) {
        ClassSectionResponse response = academicService.assignClassTeacher(sectionId, teacherId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/sections/{id}")
    @RequireAdmin
    public ResponseEntity<Void> deleteSection(@PathVariable UUID id) {
        academicService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

    // ============= SUBJECT MANAGEMENT ENDPOINTS =============

    @PostMapping("/subjects")
    @RequireAdmin
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        SubjectResponse response = academicService.createSubject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/subjects")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getAllSubjects(
            @RequestParam(required = false) UUID classId) {
        List<SubjectResponse> subjects = academicService.getAllSubjects(classId);
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @DeleteMapping("/subjects/{id}")
    @RequireAdmin
    public ResponseEntity<Void> deleteSubject(
            @PathVariable UUID id) {
        academicService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }

    // ============= TEACHER ENDPOINTS =============

    @GetMapping("/teachers")
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<UserResponse>>> getTeachers(@RequestParam(required = false) UUID schoolId) {
        List<UserResponse> teachers = academicService.getTeachers(schoolId);
        return ResponseEntity.ok(ApiResponse.success(teachers));
    }

    // ============= TIMETABLE MANAGEMENT ENDPOINTS =============

    @PostMapping("/timetable")
    @RequireAdmin
    public ResponseEntity<ApiResponse<TimetableResponse>> createTimetableEntry(
            @Valid @RequestBody CreateTimetableRequest request) {
        TimetableResponse response = academicService.createTimetableEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Timetable Created Successfully",
                response));
    }

    @PostMapping("/timetable/daily")
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<TimetableResponse>>> createDailyTimetable(
            @Valid @RequestBody CreateDailyTimetableRequest request) {
        List<TimetableResponse> responses = academicService.createDailyTimetable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Daily Timetable Created Successfully",
                responses));
    }

    @GetMapping("/timetable/section/{sectionId}")
    @RequireAdminOrTeacher
    public ResponseEntity<ApiResponse<List<TimetableResponse>>> getTimetableBySection(@PathVariable UUID sectionId) {
        List<TimetableResponse> timetables = academicService.getTimetableBySection(sectionId);
        return ResponseEntity.ok(ApiResponse.success(timetables));
    }

    @DeleteMapping("/timetable/{id}")
    @RequireAdmin
    public ResponseEntity<Void> deleteTimetableEntry(@PathVariable UUID id) {
        academicService.deleteTimetableEntry(id);
        return ResponseEntity.noContent().build();
    }
}