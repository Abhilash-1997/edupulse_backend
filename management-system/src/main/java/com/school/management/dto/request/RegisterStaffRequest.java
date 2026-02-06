package com.school.management.dto.request;

import com.school.management.constant.StaffWorkingAs;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStaffRequest {

    private UUID schoolId; // Only for SUPER_ADMIN

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String designation;

    private String department;

    @NotNull(message = "Joining date is required")
    private String joiningDate;

    @NotNull(message = "Working as is required")
    private StaffWorkingAs workingAs;
}