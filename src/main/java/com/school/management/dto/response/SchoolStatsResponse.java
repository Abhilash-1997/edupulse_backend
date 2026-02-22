package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolStatsResponse {

    private Long totalStudents;
    private Long totalTeachers;
    private Long totalClasses;
    private Long totalParents;
    private List<StudentResponse> recentStudents;
}