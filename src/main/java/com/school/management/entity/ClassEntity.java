package com.school.management.entity;

import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "classes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_classes_school_name",
                        columnNames = {"school_id", "name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_classes_school"))
    private School school;

    @Column(name = "name", nullable = false)
    private String name;
}