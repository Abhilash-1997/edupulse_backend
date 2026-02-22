package com.school.management.dto.response;

import com.school.management.constant.SchoolStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolResponse {

    private UUID id;
    private String name;
    private String address;
    private String logo;
    private String board;
    private String academicYear;
    private SchoolStatus status;
}