package com.school.management.dto.response;

import com.school.management.constant.StudyMaterialStatus;
import com.school.management.constant.StudyMaterialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterialResponse {

    private UUID id;
    private String title;
    private String description;
    private StudyMaterialType type;
    private StudyMaterialStatus status;
    private String filePath;
    private String hlsPath;
    private String originalFileName;
    private Long fileSize;
    private Integer duration;
    private Boolean isPublished;
    private String thumbnailPath;
    private Integer order;
    private UUID sectionId;
    private UUID uploadedBy;
    private String uploaderName;
}