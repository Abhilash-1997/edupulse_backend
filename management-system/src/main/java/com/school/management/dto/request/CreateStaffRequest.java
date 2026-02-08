package com.school.management.dto.request;

import com.school.management.constant.StaffWorkingAs;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    private String designation;

    private String department;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @NotNull(message = "Working as is required")
    private StaffWorkingAs workingAs;

    // Optional - for creating salary structure at the same time
    @Min(value = 0, message = "Basic salary cannot be negative")
    private Float basicSalary;
}