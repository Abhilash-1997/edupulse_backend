package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "bus_locations",
        indexes = {
                @Index(name = "idx_bus_locations_bus_timestamp", columnList = "bus_id, timestamp"),
                @Index(name = "idx_bus_locations_trip", columnList = "trip_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusLocation {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bus_locations_bus"))
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", foreignKey = @ForeignKey(name = "fk_bus_locations_trip"))
    private BusTrip trip;

    @Column(name = "lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal lng;

    @Column(name = "speed")
    private Float speed;

    @Column(name = "heading")
    private Float heading;

    @Column(name = "accuracy")
    private Float accuracy;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}