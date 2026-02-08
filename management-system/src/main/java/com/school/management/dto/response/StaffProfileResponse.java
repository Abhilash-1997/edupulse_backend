package com.school.management.dto.response;

import com.school.management.constant.StaffStatus;
import com.school.management.constant.StaffWorkingAs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffProfileResponse {

    private UUID id;
    private String employeeCode;
    private String department;
    private String designation;
    private LocalDate joiningDate;
    private StaffWorkingAs workingAs;
    private StaffStatus status;
    private UserResponse user;
}