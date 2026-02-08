package com.school.management.security;

import com.school.management.constant.UserRole;
import com.school.management.entity.StaffProfile;
import com.school.management.repository.StaffProfileRepository;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("financeAccess")
@RequiredArgsConstructor
public class FinanceAccessControl {

    private final StaffProfileRepository staffProfileRepository;

    /**
     * Check if user has access to fee collection features
     * Logic matches Express restrictToFeeCollection middleware
     */
    public boolean hasAccess() {
        UserRole role = SecurityUtils.getCurrentUserRole();

        // 1. Allow Super Admin and School Admin immediately
        if (role == UserRole.SUPER_ADMIN || role == UserRole.SCHOOL_ADMIN) {
            return true;
        }

        // 2. If not STAFF role, deny
        if (role != UserRole.STAFF) {
            return false;
        }

        // 3. For STAFF, check Department
        UUID userId = SecurityUtils.getCurrentUserId();
        StaffProfile staffProfile = staffProfileRepository.findByUser_IdAndDeletedAtIsNull(userId)
                .orElse(null);

        if (staffProfile == null) {
            return false;
        }

        // Normalize department check (case-insensitive)
        String department = staffProfile.getDepartment();
        if (department == null) {
            return false;
        }

        return "ACCOUNTS".equalsIgnoreCase(department.trim());
    }
}