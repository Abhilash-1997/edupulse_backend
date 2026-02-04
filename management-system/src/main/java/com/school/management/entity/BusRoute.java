package com.school.management.entity;

import com.school.management.constant.BusRouteType;
import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(
        name = "bus_routes",
        indexes = {
                @Index(name = "idx_bus_routes_school_bus", columnList = "school_id, bus_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusRoute extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bus_routes_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bus_routes_bus"))
    private Bus bus;

    @Column(name = "route_name", nullable = false)
    private String routeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false)
    @Builder.Default
    private BusRouteType routeType = BusRouteType.BOTH;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stops", nullable = false, columnDefinition = "JSON")
    @Builder.Default
    private List<Map<String, Object>> stops = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}