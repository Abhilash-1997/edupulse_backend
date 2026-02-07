package com.school.management.service;

import com.school.management.constant.SchoolStatus;
import com.school.management.constant.StaffStatus;
import com.school.management.constant.UserRole;
import com.school.management.dto.request.LoginRequest;
import com.school.management.dto.request.RegisterSchoolRequest;
import com.school.management.dto.request.RegisterStaffRequest;
import com.school.management.dto.request.UpdatePasswordRequest;
import com.school.management.dto.response.*;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.exception.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.security.JwtTokenProvider;
import com.school.management.config.PasswordUtil;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final ParentRepository parentRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordUtil passwordUtil;
//    private final EmailService emailService;

    @Transactional
    public AuthResponse registerSchool(RegisterSchoolRequest request) {
        // Check if admin email already exists
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getAdminEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Create School
        School school = School.builder()
                .name(request.getSchoolName())
                .address(request.getSchoolAddress())
                .board(request.getSchoolBoard())
                .status(SchoolStatus.PENDING)
                .build();
        school = schoolRepository.save(school);

        // Create School Admin User
        User adminUser = User.builder()
                .school(school)
                .name(request.getAdminName())
                .email(request.getAdminEmail())
                .phone(request.getAdminPhone())
                .passwordHash(passwordUtil.hashPassword(request.getAdminPassword()))
                .role(UserRole.SCHOOL_ADMIN)
                .isActive(true)
                .build();
        adminUser = userRepository.save(adminUser);

        // Send welcome email
//        try {
//            emailService.sendSchoolRegistrationEmail(
//                    school.getName(),
//                    adminUser.getName(),
//                    adminUser.getEmail(),
//                    school.getId().toString()
//            );
//        } catch (Exception e) {
//            log.error("Failed to send school registration email", e);
//        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(adminUser.getId());

        return buildAuthResponse(token, adminUser, school);
    }

    @Transactional
    public AuthResponse registerStaff(RegisterStaffRequest request) {
        UUID schoolId = request.getSchoolId();

        // If not SUPER_ADMIN, use current user's school
        if (!SecurityUtils.isSuperAdmin()) {
            schoolId = SecurityUtils.getCurrentUserSchoolId();
        }

        if (schoolId == null) {
            throw new BadRequestException("School ID is required");
        }

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Check duplicate email globally
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        // Map working role to user role
        UserRole userRole = switch (request.getWorkingAs()) {
            case TEACHER -> UserRole.TEACHER;
            case LIBRARIAN -> UserRole.LIBRARIAN;
            case BUS_DRIVER -> UserRole.BUS_DRIVER;
            default -> UserRole.STAFF;
        };

        // Create User
        User user = User.builder()
                .school(school)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordUtil.hashPassword(request.getPassword()))
                .role(userRole)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        // Generate employee code
        String employeeCode = "EMP-" + String.valueOf(System.currentTimeMillis()).substring(7);

        // Parse joining date
        LocalDate joiningDate = LocalDate.parse(request.getJoiningDate());

        // Create Staff Profile
        StaffProfile staffProfile = StaffProfile.builder()
                .school(school)
                .user(user)
                .employeeCode(employeeCode)
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .joiningDate(joiningDate)
                .workingAs(request.getWorkingAs())
                .status(StaffStatus.PRE_BOARDING)
                .build();
        staffProfile = staffProfileRepository.save(staffProfile);

        // Send welcome email
//        try {
//            emailService.sendStaffCreationEmail(
//                    school.getName(),
//                    user.getName(),
//                    user.getEmail(),
//                    request.getPassword(),
//                    request.getDesignation(),
//                    request.getDepartment()
//            );
//        } catch (Exception e) {
//            log.error("Failed to send staff creation email", e);
//        }

        String token = jwtTokenProvider.generateToken(user.getId());

        return buildAuthResponse(token, user, school);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Verify password
        if (!passwordUtil.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Check if user is active
        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        // Generate token
        String token = jwtTokenProvider.generateToken(user.getId());

        return buildAuthResponse(token, user, user.getSchool());
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordUtil.verifyPassword(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Update to new password
        user.setPasswordHash(passwordUtil.hashPassword(request.getNewPassword()));
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(String token, User user, School school) {
        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .school(mapToSchoolResponse(school));

        // Add staff profile if applicable
        if (user.getRole() == UserRole.TEACHER ||
                user.getRole() == UserRole.STAFF ||
                user.getRole() == UserRole.LIBRARIAN ||
                user.getRole() == UserRole.BUS_DRIVER) {

            staffProfileRepository.findByUser_IdAndDeletedAtIsNull(user.getId())
                    .ifPresent(staff -> builder.staffProfile(mapToStaffProfileResponse(staff)));
        }

        // Add parent profile if applicable
        if (user.getRole() == UserRole.PARENT) {
            parentRepository.findByUser_IdAndDeletedAtIsNull(user.getId())
                    .ifPresent(parent -> builder.parent(mapToParentResponse(parent)));
        }

        return builder.build();
    }

    // Mapper methods (simplified - ideally use MapStruct)
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .schoolId(user.getSchool() != null ? user.getSchool().getId() : null)
                .build();
    }

    private SchoolResponse mapToSchoolResponse(School school) {
        if (school == null) return null;

        return SchoolResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .address(school.getAddress())
                .logo(school.getLogo())
                .board(school.getBoard())
                .academicYear(school.getAcademicYear())
                .status(school.getStatus())
                .build();
    }

    private StaffProfileResponse mapToStaffProfileResponse(StaffProfile staff) {
        return StaffProfileResponse.builder()
                .id(staff.getId())
                .employeeCode(staff.getEmployeeCode())
                .department(staff.getDepartment())
                .designation(staff.getDesignation())
                .joiningDate(staff.getJoiningDate())
                .workingAs(staff.getWorkingAs())
                .status(staff.getStatus())
                .build();
    }

    private ParentResponse mapToParentResponse(Parent parent) {
        return ParentResponse.builder()
                .id(parent.getId())
                .guardianName(parent.getGuardianName())
                .occupation(parent.getOccupation())
                .build();
    }
}