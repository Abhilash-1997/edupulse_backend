package com.school.management.entity;

import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(
        name = "student_bus_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_bus_assignments_student_active",
                        columnNames = {"student_id"}
                )
        },
        indexes = {
                @Index(name = "idx_student_bus_assignments_bus", columnList = "bus_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentBusAssignment extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_bus_assignments_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_bus_assignments_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_bus_assignments_bus"))
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", foreignKey = @ForeignKey(name = "fk_student_bus_assignments_route"))
    private BusRoute route;

    @Column(name = "stop_name")
    private String stopName;

    @Column(name = "pickup_time")
    private LocalTime pickupTime;

    @Column(name = "dropoff_time")
    private LocalTime dropoffTime;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}