package com.school.management.controller;

import com.school.management.dto.request.LoginRequest;
import com.school.management.dto.request.RegisterSchoolRequest;
import com.school.management.dto.request.RegisterStaffRequest;
import com.school.management.dto.request.UpdatePasswordRequest;
import com.school.management.dto.response.AuthResponse;
import com.school.management.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-school")
    public ResponseEntity<AuthResponse> registerSchool(@Valid @RequestBody RegisterSchoolRequest request) {
        AuthResponse response = authService.registerSchool(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/register-staff")
    public ResponseEntity<AuthResponse> registerStaff(@Valid @RequestBody RegisterStaffRequest request) {
        AuthResponse response = authService.registerStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
