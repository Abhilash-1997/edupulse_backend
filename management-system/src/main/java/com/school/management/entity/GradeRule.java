package com.school.management.entity;

import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "grade_rules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_grade_rules_school_grade",
                        columnNames = {"school_id", "grade"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeRule extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_grade_rules_school"))
    private School school;

    @Column(name = "grade", nullable = false)
    private String grade;

    @Column(name = "min_percentage", nullable = false)
    private Float minPercentage;

    @Column(name = "max_percentage", nullable = false)
    private Float maxPercentage;

    @Column(name = "description")
    private String description;
}