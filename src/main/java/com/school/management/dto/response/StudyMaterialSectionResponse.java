package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterialSectionResponse {

    private UUID id;
    private String title;
    private String description;
    private Boolean isPublished;
    private Integer order;
    private UUID classId;
    private String className;
    private UUID sectionId;
    private String sectionName;
    private UUID subjectId;
    private String subjectName;
    private UUID createdBy;
    private String creatorName;
    private List<StudyMaterialResponse> materials;
}