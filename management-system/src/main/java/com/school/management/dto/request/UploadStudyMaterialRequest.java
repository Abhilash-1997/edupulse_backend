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
public class UploadStudyMaterialRequest {

    private String title;
    private String description;
    private Boolean isPublished;
    private UUID sectionId;

    // File will be handled via MultipartFile in controller
}