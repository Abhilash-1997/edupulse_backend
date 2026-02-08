package com.school.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudyMaterialSectionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Class ID is required")
    private UUID classId;

    private UUID sectionId;

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    private Boolean isPublished;
}