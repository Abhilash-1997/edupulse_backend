package com.school.management.dto.response;

import com.school.management.constant.SchoolStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolInfoResponse {

    private UUID id;
    private String name;
    private String address;
    private String board;
    private SchoolStatus status;
    private String schoolAdminEmail;
    private Long studentsCount;
    private Long teachersCount;
    private Long staffCount;
    private Long classesCount;
    private Long subjectsCount;
    private Long examsCount;
    private LocalDateTime createdAt;
}