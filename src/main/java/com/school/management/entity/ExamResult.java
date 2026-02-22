package com.school.management.entity;

import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "exam_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_exam_results_exam_student_subject",
                        columnNames = {"exam_id", "student_id", "subject_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exam_results_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exam_results_exam"))
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exam_results_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exam_results_subject"))
    private Subject subject;

    @Column(name = "marks_obtained", nullable = false)
    @Builder.Default
    private Float marksObtained = 0.0f;

    @Column(name = "max_marks", nullable = false)
    @Builder.Default
    private Float maxMarks = 100.0f;

    @Column(name = "grade")
    private String grade;

    @Column(name = "remarks")
    private String remarks;
}