package com.school.management.controller;

import com.school.management.dto.request.BulkUpdateStudentsRequest;
import com.school.management.dto.request.CreateStudentRequest;
import com.school.management.dto.request.UpdateStudentRequest;
import com.school.management.dto.response.StudentResponse;
import com.school.management.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    //===================   CREATE STUDENT ============================

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest createStudentRequest) {
        StudentResponse studentResponse = studentService.createStudent(createStudentRequest);
        return ResponseEntity.ok(studentResponse);
    }

    //===================   UPDATE STUDENT ============================

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable UUID id, @Valid @RequestBody UpdateStudentRequest request) {

        StudentResponse response = studentService.updateStudent(id, request);
        return ResponseEntity.ok(response);
    }

    //===================   BULK UPDATE STUDENT ============================

    @PutMapping("/bulk-update")
    public ResponseEntity<Void> bulkUpdateStudents(@Valid @RequestBody BulkUpdateStudentsRequest request) {
        studentService.bulkUpdateStudents(request);
        return ResponseEntity.ok().build();
    }

    //===================   GET ALL STUDENT ============================

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents(@RequestParam(required = false) UUID parentId, @RequestParam(required = false) UUID classId) {
        List<StudentResponse> response = studentService.getAllStudents(parentId, classId);
        return ResponseEntity.ok().body(response);
    }

    //===================   GET ALL STUDENT ============================

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentsById(@PathVariable UUID id) {
        StudentResponse response = studentService.getStudentById(id);
        return ResponseEntity.ok().body(response);
    }



    //===================   UPLOAD PROFILE PIC ============================

    @PostMapping("/{id}/profile-picture")
    public ResponseEntity<String> uploadProfilePicture(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        String path = studentService.uploadProfilePicture(id, file);
        return ResponseEntity.ok(path);
    }

    //=================== DELETE STUDENT ============================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable UUID id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }



}
