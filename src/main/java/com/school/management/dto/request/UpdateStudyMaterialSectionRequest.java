package com.school.management.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudyMaterialSectionRequest {

    private String title;
    private String description;
    private UUID classId;
    private UUID sectionId;
    private UUID subjectId;
    private Boolean isPublished;
    private Integer order;
}