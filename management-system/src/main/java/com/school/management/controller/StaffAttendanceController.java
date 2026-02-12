package com.school.management.controller;

import com.school.management.dto.request.MarkStaffAttendanceRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.StaffAttendanceResponse;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.service.StaffAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/staff-attendance")
@RequiredArgsConstructor
@RequireAdmin
public class StaffAttendanceController {

    private final StaffAttendanceService staffAttendanceService;

    @PostMapping("/mark")
    public ResponseEntity<ApiResponse<List<StaffAttendanceResponse>>> markAttendance(
            @Valid @RequestBody MarkStaffAttendanceRequest request){

        List<StaffAttendanceResponse> response = staffAttendanceService.markAttendance(request);
        return ResponseEntity.ok(ApiResponse.success("Attendance marked successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffAttendanceResponse>>> getAttendance(
            @RequestParam(required = true) LocalDate date){
        List<StaffAttendanceResponse> response = staffAttendanceService.getAttendanceByDate(date);
        return ResponseEntity.ok(ApiResponse.success("Attendance Fetched successfully on " + date , response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<List<StaffAttendanceResponse>>> getAttendance(
            @PathVariable UUID id,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate){
            List<StaffAttendanceResponse> response = staffAttendanceService.getStaffAttendance(id, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("StaffAttendance Fetched successfully on " + startDate + " and " + endDate , response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffAttendanceResponse>> updateAttendance(
            @PathVariable UUID id,
            @Valid @RequestBody MarkStaffAttendanceRequest.StaffAttendanceData data) {

        StaffAttendanceResponse response = staffAttendanceService.updateAttendance(id, data);
        return ResponseEntity.ok(ApiResponse.success("Staff attendance updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(@PathVariable UUID id){
        staffAttendanceService.deleteAttendance(id);
        return ResponseEntity.ok().build();
    }




}
