package com.school.management.service;

import com.school.management.constant.SchoolStatus;
import com.school.management.constant.StaffStatus;
import com.school.management.constant.UserRole;
import com.school.management.dto.request.*;
import com.school.management.dto.response.*;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.exception.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.security.JwtTokenProvider;
import com.school.management.util.PasswordUtil;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final ParentRepository parentRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

        try {
            emailService.sendSchoolRegistrationEmail(
                    school.getName(),
                    adminUser.getName(),
                    adminUser.getEmail(),
                    school.getId().toString());
        } catch (Exception e) {
            log.error("Failed to send school registration email", e);
        }

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
        try {
            emailService.sendStaffCreationEmail(
                    school.getName(),
                    user.getName(),
                    user.getEmail(),
                    request.getPassword(),
                    request.getDesignation(),
                    request.getDepartment());
        } catch (Exception e) {
            log.error("Failed to send staff creation email", e);
        }

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

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to avoid leaking whether an account exists
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElse(null);

        if (user == null) {
            log.warn("Forgot password requested for non-existent email: {}", request.getEmail());
            return;
        }

        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Create and save token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Send email
        try {
            String schoolName = user.getSchool() != null ? user.getSchool().getName() : "EduPulse";
            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getName(),
                    otp,
                    schoolName);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }

        log.info("Password reset OTP generated for user: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Find the token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP code"));

        // Check if token is expired
        if (resetToken.isExpired()) {
            throw new BadRequestException("OTP code has expired. Please request a new one");
        }

        // Verify the email matches
        User user = resetToken.getUser();
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Invalid or expired OTP code");
        }

        // Update password
        user.setPasswordHash(passwordUtil.hashPassword(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    private String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private AuthResponse buildAuthResponse(String token, User user, School school) {
        UserResponse userResponse = mapToUserResponse(user);

        // Nest school inside user (matches Node.js: user.School)
        userResponse.setSchool(mapToSchoolResponse(school));

        // Add staff profile if applicable (matches Node.js: user.staffProfile)
        if (user.getRole() == UserRole.SCHOOL_ADMIN ||
                user.getRole() == UserRole.TEACHER ||
                user.getRole() == UserRole.STAFF ||
                user.getRole() == UserRole.LIBRARIAN ||
                user.getRole() == UserRole.BUS_DRIVER) {

            staffProfileRepository.findByUser_IdAndDeletedAtIsNull(user.getId())
                    .ifPresent(staff -> userResponse.setStaffProfile(mapToStaffProfileResponse(staff)));
        }

        // Add parent profile if applicable (matches Node.js: user.parent)
        if (user.getRole() == UserRole.PARENT) {
            parentRepository.findByUser_IdAndDeletedAtIsNull(user.getId())
                    .ifPresent(parent -> userResponse.setParent(mapToParentResponse(parent)));
        }

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .school(mapToSchoolResponse(school))
                .staffProfile(userResponse.getStaffProfile())
                .parent(userResponse.getParent())
                .build();
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
        if (school == null)
            return null;

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