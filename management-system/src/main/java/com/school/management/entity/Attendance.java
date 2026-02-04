package com.school.management.entity;

import com.school.management.constant.AttendanceStatus;
import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "attendances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_school_student_date",
                        columnNames = {"school_id", "student_id", "date"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_section"))
    private ClassSection section;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", foreignKey = @ForeignKey(name = "fk_attendance_recorder"))
    private User recordedBy;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;
}