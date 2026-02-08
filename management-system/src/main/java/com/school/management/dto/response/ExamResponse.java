package com.school.management.dto.response;

import com.school.management.constant.ExamType;
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
public class ExamResponse {

    private UUID id;
    private String name;
    private ExamType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private ClassResponse classInfo;
    private ClassSectionResponse section;
}