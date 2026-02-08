package com.school.management.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudyMaterialRequest {

    private String title;
    private String description;
    private Boolean isPublished;
    private Integer order;
}