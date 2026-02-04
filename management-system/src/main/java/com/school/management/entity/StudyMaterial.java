package com.school.management.entity;

import com.school.management.constant.StudyMaterialStatus;
import com.school.management.constant.StudyMaterialType;
import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyMaterial extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_material_section_id", nullable = false, foreignKey = @ForeignKey(name = "fk_study_materials_section"))
    private StudyMaterialSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_study_materials_school"))
    private School school;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private StudyMaterialType type;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StudyMaterialStatus status = StudyMaterialStatus.COMPLETED;

    @Column(name = "hls_path")
    private String hlsPath;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "duration")
    private Integer duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false, foreignKey = @ForeignKey(name = "fk_study_materials_uploader"))
    private User uploader;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "`order`", nullable = false)
    @Builder.Default
    private Integer order = 0;
}