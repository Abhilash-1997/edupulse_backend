package com.school.management.entity;

import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gallery_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryImage extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gallery_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gallery_images_gallery"))
    private Gallery gallery;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;
}