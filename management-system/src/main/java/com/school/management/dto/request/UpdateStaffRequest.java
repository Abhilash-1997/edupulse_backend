package com.school.management.dto.request;

import com.school.management.constant.StaffWorkingAs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaffRequest {

    private String name;
    private String email;
    private String phone;
    private String designation;
    private String department;
    private StaffWorkingAs workingAs;
}