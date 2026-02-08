package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRuleResponse {

    private UUID id;
    private String grade;
    private Float minPercentage;
    private Float maxPercentage;
    private String description;
}