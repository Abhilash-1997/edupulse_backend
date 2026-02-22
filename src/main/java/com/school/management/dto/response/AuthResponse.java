package com.school.management.dto.response;

import com.school.management.constant.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UserResponse user;
    private SchoolResponse school;
    private StaffProfileResponse staffProfile;
    private ParentResponse parent;
}