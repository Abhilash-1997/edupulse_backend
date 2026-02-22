package com.school.management.util;

import com.school.management.constant.UserRole;
import com.school.management.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    /**
     * Get current authenticated user principal
     */
    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }

        return null;
    }

    /**
     * Get current user ID
     */
    public static UUID getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Get current user's school ID
     */
    public static UUID getCurrentUserSchoolId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getSchoolId() : null;
    }

    /**
     * Get current user's role
     */
    public static UserRole getCurrentUserRole() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(UserRole role) {
        UserPrincipal user = getCurrentUser();
        return user != null && user.getRole() == role;
    }

    /**
     * Check if current user has any of the specified roles
     */
    public static boolean hasAnyRole(UserRole... roles) {
        UserPrincipal user = getCurrentUser();
        if (user == null) {
            return false;
        }

        for (UserRole role : roles) {
            if (user.getRole() == role) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if current user is admin (SUPER_ADMIN or SCHOOL_ADMIN)
     */
    public static boolean isAdmin() {
        return hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SCHOOL_ADMIN);
    }

    /**
     * Check if current user is SUPER_ADMIN
     */
    public static boolean isSuperAdmin() {
        return hasRole(UserRole.SUPER_ADMIN);
    }
}