package com.school.management.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private Boolean isActive;
    private UUID schoolId;

    // Nested profile data (populated during login/register)
    private SchoolResponse school;
    private StaffProfileResponse staffProfile;
    private ParentResponse parent;
}