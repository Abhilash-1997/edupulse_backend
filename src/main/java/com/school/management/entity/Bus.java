package com.school.management.entity;

import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "buses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_buses_school_number",
                        columnNames = {"school_id", "bus_number"}
                ),
                @UniqueConstraint(
                        name = "uk_buses_school_registration",
                        columnNames = {"school_id", "registration_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bus extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_buses_school"))
    private School school;

    @Column(name = "bus_number", nullable = false)
    private String busNumber;

    @Column(name = "registration_number", nullable = false)
    private String registrationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", foreignKey = @ForeignKey(name = "fk_buses_driver"))
    private User driver;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "capacity")
    @Builder.Default
    private Integer capacity = 40;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}