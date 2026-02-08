package com.school.management.controller;

import com.school.management.constant.UserRole;
import com.school.management.dto.request.CreateStaffRequest;
import com.school.management.dto.request.UpdateStaffRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.StaffProfileResponse;
import com.school.management.security.annotation.RequireAdmin;
import com.school.management.service.StaffService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    @RequireAdmin
    public ResponseEntity<ApiResponse<StaffProfileResponse>> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {
        StaffProfileResponse response = staffService.createStaff(request);
        return ResponseEntity.ok(ApiResponse.success("Staff created successfully", response));
    }

    @GetMapping
    @RequireAdmin
    public ResponseEntity<ApiResponse<List<StaffProfileResponse>>> getAllStaff(
            @RequestParam(required = false) List<UserRole> roles,
            @RequestParam(required = false) Boolean isActive) {
        List<StaffProfileResponse> staffList = staffService.getAllStaff(roles, isActive);
        return ResponseEntity.ok(ApiResponse.success("Staff retrieved successfully", staffList));
    }

    @GetMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getStaffById(@PathVariable UUID id) {
        StaffProfileResponse response = staffService.getStaffById(id);
        return ResponseEntity.ok(ApiResponse.success("Staff retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest request) {
        StaffProfileResponse response = staffService.updateStaff(id, request);
        return ResponseEntity.ok(ApiResponse.success("Staff updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable UUID id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok(ApiResponse.success("Staff deleted successfully", null));
    }

    @PostMapping("/{id}/accept-offer")
    public ResponseEntity<ApiResponse<Void>> acceptOffer(@PathVariable("id") UUID staffId) {
        staffService.acceptOffer(staffId);
        return ResponseEntity.ok(ApiResponse.success("Offer accepted successfully", null));
    }

    @GetMapping("/{id}/offer-letter")
    @RequireAdmin
    public ResponseEntity<byte[]> generateOfferLetter(
            @PathVariable UUID id,
            HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        byte[] pdfBytes = staffService.generateOfferLetter(id, baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Offer_Letter.pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/{id}/joining-letter")
    @RequireAdmin
    public ResponseEntity<byte[]> generateJoiningLetter(
            @PathVariable UUID id,
            HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        byte[] pdfBytes = staffService.generateJoiningLetter(id, baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Joining_Letter.pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
