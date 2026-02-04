package com.school.management.entity;

import com.school.management.constant.BusTripStatus;
import com.school.management.constant.BusTripType;
import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "bus_trips",
        indexes = {
                @Index(name = "idx_bus_trips_school_bus_status", columnList = "school_id, bus_id, status"),
                @Index(name = "idx_bus_trips_start_time", columnList = "start_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusTrip extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bus_trips_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bus_trips_bus"))
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", foreignKey = @ForeignKey(name = "fk_bus_trips_route"))
    private BusRoute route;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false)
    @Builder.Default
    private BusTripType tripType = BusTripType.MORNING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BusTripStatus status = BusTripStatus.NOT_STARTED;

    @Column(name = "last_reached_stop_order", nullable = false)
    @Builder.Default
    private Integer lastReachedStopOrder = 0;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}