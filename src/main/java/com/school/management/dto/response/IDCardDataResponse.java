package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IDCardDataResponse {

    private String schoolName;
    private String schoolAddress;
    private String schoolLogo;
    private String studentName;
    private String admissionNumber;
    private String classInfo;
    private String dob;
    private String bloodGroup;
    private String fatherName;
    private String emergencyContact;
}