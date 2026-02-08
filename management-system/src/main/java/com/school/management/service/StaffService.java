package com.school.management.service;

import com.school.management.constant.StaffStatus;
import com.school.management.constant.StaffWorkingAs;
import com.school.management.constant.UserRole;
import com.school.management.dto.request.CreateStaffRequest;
import com.school.management.dto.request.UpdateStaffRequest;
import com.school.management.dto.response.StaffProfileResponse;
import com.school.management.dto.response.UserResponse;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.util.PasswordUtil;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;
    private final PdfGenerationService pdfGenerationService;

    @Transactional
    public StaffProfileResponse createStaff(CreateStaffRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Validate email globally (not just in school)
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        // Map role
        UserRole userRole = switch (request.getWorkingAs()) {
            case TEACHER -> UserRole.TEACHER;
            case LIBRARIAN -> UserRole.LIBRARIAN;
            case BUS_DRIVER -> UserRole.BUS_DRIVER;
            default -> UserRole.STAFF;
        };

        // Generate employee code
        String employeeCode = "EMP-" + String.valueOf(System.currentTimeMillis()).substring(7);

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

        // Create Staff Profile
        StaffProfile staffProfile = StaffProfile.builder()
                .school(school)
                .user(user)
                .employeeCode(employeeCode)
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .joiningDate(request.getJoiningDate())
                .workingAs(request.getWorkingAs())
                .status(StaffStatus.PRE_BOARDING)
                .build();
        staffProfile = staffProfileRepository.save(staffProfile);

        // Create Salary Structure if basicSalary provided
        if (request.getBasicSalary() != null) {
            SalaryStructure salaryStructure = SalaryStructure.builder()
                    .school(school)
                    .staff(staffProfile)
                    .basicSalary(request.getBasicSalary())
                    .netSalary(request.getBasicSalary())
                    .effectiveDate(LocalDate.now())
                    .build();
            salaryStructureRepository.save(salaryStructure);
        }

        // Send email with raw password
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

        return mapToStaffProfileResponse(staffProfile);
    }

    @Transactional(readOnly = true)
    public List<StaffProfileResponse> getAllStaff(List<UserRole> roles, Boolean isActive) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        // Default roles if not provided
        if (roles == null || roles.isEmpty()) {
            roles = List.of(UserRole.TEACHER, UserRole.STAFF, UserRole.LIBRARIAN, UserRole.BUS_DRIVER);
        }

        List<StaffProfile> staffProfiles = staffProfileRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);

        // Filter by status (isActive maps to status ACTIVE)
        if (isActive != null && isActive) {
            staffProfiles = staffProfiles.stream()
                    .filter(staff -> staff.getStatus() == StaffStatus.ACTIVE)
                    .collect(Collectors.toList());
        }

        // Filter by roles
        List<UserRole> finalRoles = roles;
        staffProfiles = staffProfiles.stream()
                .filter(staff -> finalRoles.contains(staff.getUser().getRole()))
                .collect(Collectors.toList());

        return staffProfiles.stream()
                .map(this::mapToStaffProfileResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StaffProfileResponse getStaffById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StaffProfile staffProfile = staffProfileRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        return mapToStaffProfileResponse(staffProfile);
    }

    @Transactional
    public StaffProfileResponse updateStaff(UUID id, UpdateStaffRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StaffProfile staffProfile = staffProfileRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        User user = staffProfile.getUser();

        // Update User fields
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        // Sync role with workingAs
        if (request.getWorkingAs() != null) {
            UserRole newRole = switch (request.getWorkingAs()) {
                case TEACHER -> UserRole.TEACHER;
                case LIBRARIAN -> UserRole.LIBRARIAN;
                case BUS_DRIVER -> UserRole.BUS_DRIVER;
                default -> UserRole.STAFF;
            };
            user.setRole(newRole);
        }

        userRepository.save(user);

        // Update StaffProfile fields
        if (request.getDesignation() != null) {
            staffProfile.setDesignation(request.getDesignation());
        }
        if (request.getDepartment() != null) {
            staffProfile.setDepartment(request.getDepartment());
        }
        if (request.getWorkingAs() != null) {
            staffProfile.setWorkingAs(request.getWorkingAs());
        }

        staffProfile = staffProfileRepository.save(staffProfile);

        return mapToStaffProfileResponse(staffProfile);
    }

    @Transactional
    public void deleteStaff(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StaffProfile staffProfile = staffProfileRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        // Soft deactivation
        User user = staffProfile.getUser();
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void acceptOffer(UUID staffId) {
        StaffProfile staffProfile = staffProfileRepository.findByIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        if (staffProfile.getStatus() != StaffStatus.PRE_BOARDING) {
            throw new BadRequestException("Offer can only be accepted in PRE_BOARDING status");
        }

        staffProfile.setStatus(StaffStatus.ACTIVE);
        staffProfileRepository.save(staffProfile);
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
                .user(mapToUserResponse(staff.getUser()))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build();
    }

    /**
     * Generates an Offer Letter PDF for a staff member.
     *
     * @param staffProfileId the staff profile ID
     * @param baseUrl        the base URL for assets (e.g., school logo)
     * @return byte array containing the PDF
     */
    @Transactional(readOnly = true)
    public byte[] generateOfferLetter(UUID staffProfileId, String baseUrl) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StaffProfile staffProfile = staffProfileRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(staffProfileId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        User user = staffProfile.getUser();
        School school = staffProfile.getSchool();

        // Get salary structure
        SalaryStructure salaryStructure = salaryStructureRepository.findByStaff_IdAndDeletedAtIsNull(staffProfileId)
                .orElse(null);

        // Prepare currency formatter for INR
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Calculate total earnings
        float totalEarnings = 0;
        List<Map<String, String>> formattedAllowances = new ArrayList<>();

        if (salaryStructure != null) {
            totalEarnings = salaryStructure.getBasicSalary();

            if (salaryStructure.getAllowances() != null) {
                for (Map<String, Object> allowance : salaryStructure.getAllowances()) {
                    String name = (String) allowance.get("name");
                    Object amountObj = allowance.get("amount");
                    float amount = 0;
                    if (amountObj instanceof Number) {
                        amount = ((Number) amountObj).floatValue();
                    }
                    totalEarnings += amount;

                    Map<String, String> formatted = new HashMap<>();
                    formatted.put("name", name);
                    formatted.put("formattedAmount", currencyFormat.format(amount));
                    formattedAllowances.add(formatted);
                }
            }
        }

        // Date formatter
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

        // Prepare template variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("schoolName", school.getName());
        variables.put("schoolAddress", school.getAddress());
        variables.put("schoolEmail", null); // Not in School entity
        variables.put("schoolPhone", null); // Not in School entity
        variables.put("schoolLogo", school.getLogo() != null ? baseUrl + "/api/" + school.getLogo() : null);

        variables.put("currentYear", String.valueOf(LocalDate.now().getYear()));
        variables.put("currentDate", LocalDate.now().format(dateFormatter));
        variables.put("employeeCode", staffProfile.getEmployeeCode());

        variables.put("staffName", user.getName());
        variables.put("staffAddress", null); // StaffProfile doesn't have address field
        variables.put("staffEmail", user.getEmail());

        variables.put("designation", staffProfile.getDesignation());
        variables.put("department", staffProfile.getDepartment());
        variables.put("joiningDate", staffProfile.getJoiningDate().format(dateFormatter));

        variables.put("hasSalaryStructure", salaryStructure != null);
        if (salaryStructure != null) {
            variables.put("formattedBasicSalary", currencyFormat.format(salaryStructure.getBasicSalary()));
            variables.put("allowances", formattedAllowances);
            variables.put("formattedTotalEarnings", currencyFormat.format(totalEarnings));
        }

        return pdfGenerationService.generatePdf("letters/offer-letter", variables);
    }

    /**
     * Generates a Joining Letter PDF for a staff member.
     *
     * @param staffProfileId the staff profile ID
     * @param baseUrl        the base URL for assets (e.g., school logo)
     * @return byte array containing the PDF
     */
    @Transactional(readOnly = true)
    public byte[] generateJoiningLetter(UUID staffProfileId, String baseUrl) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        StaffProfile staffProfile = staffProfileRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(staffProfileId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        User user = staffProfile.getUser();
        School school = staffProfile.getSchool();

        // Date formatter
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

        // Prepare template variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("schoolName", school.getName());
        variables.put("schoolAddress", school.getAddress());
        variables.put("schoolEmail", null); // Not in School entity
        variables.put("schoolPhone", null); // Not in School entity
        variables.put("schoolLogo", school.getLogo() != null ? baseUrl + "/api/" + school.getLogo() : null);

        variables.put("currentYear", String.valueOf(LocalDate.now().getYear()));
        variables.put("currentDate", LocalDate.now().format(dateFormatter));
        variables.put("employeeCode", staffProfile.getEmployeeCode());

        variables.put("staffName", user.getName());
        variables.put("staffEmail", user.getEmail());
        variables.put("staffPhone", user.getPhone());

        variables.put("designation", staffProfile.getDesignation());
        variables.put("department", staffProfile.getDepartment());
        variables.put("joiningDate", staffProfile.getJoiningDate().format(dateFormatter));

        return pdfGenerationService.generatePdf("letters/joining-letter", variables);
    }
}