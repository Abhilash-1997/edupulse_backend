package com.school.management.controller;

import com.school.management.constant.LeaveStatus;
import com.school.management.dto.request.ApplyLeaveRequest;
import com.school.management.dto.request.UpdateLeaveStatusRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.LeaveResponse;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LeaveResponse>> applyLeave(@Valid @RequestBody ApplyLeaveRequest request){
        LeaveResponse leaveResponse = leaveService.applyLeave(request);
        return ResponseEntity.ok(ApiResponse.success("Leave Applied Successfully",leaveResponse));
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getAllLeaves(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) String role) {
        List<LeaveResponse> response = leaveService.getAllLeaves(status, role);
        return ResponseEntity.ok(ApiResponse.success("All leaves fetched successfully", response)
        );
    }

    @GetMapping("/my-leaves")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getMyLeaves(
            @RequestParam(required = false) LeaveStatus status) {

        List<LeaveResponse> response = leaveService.getMyLeaves(status);
        return ResponseEntity.ok(ApiResponse.success("My leaves fetched successfully", response));
    }

    @PutMapping("/{id}/status")
    @RequireAdmin
    public ResponseEntity<ApiResponse<LeaveResponse>> updateLeaveStatus(@PathVariable UUID id, @Valid @RequestBody UpdateLeaveStatusRequest request) {
        LeaveResponse response = leaveService.updateLeaveStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Leave status updated successfully", response));
    }
}
