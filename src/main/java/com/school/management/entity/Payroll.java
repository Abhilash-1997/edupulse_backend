package com.school.management.entity;

import com.school.management.constant.PayrollStatus;
import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(
        name = "payrolls",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payroll_school_staff_month_year",
                        columnNames = {"school_id", "staff_id", "month", "year"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payroll extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payrolls_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payrolls_staff"))
    private StaffProfile staff;

    @Column(name = "month", nullable = false)
    private String month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "basic_salary", nullable = false)
    private Float basicSalary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowances", columnDefinition = "JSON")
    @Builder.Default
    private List<Map<String, Object>> allowances = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deductions_breakdown", columnDefinition = "JSON")
    @Builder.Default
    private Map<String, Object> deductionsBreakdown = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attendance_summary", columnDefinition = "JSON")
    @Builder.Default
    private Map<String, Object> attendanceSummary = new HashMap<>();

    @Column(name = "bonus")
    @Builder.Default
    private Float bonus = 0.0f;

    @Column(name = "deductions")
    @Builder.Default
    private Float deductions = 0.0f;

    @Column(name = "net_salary", nullable = false)
    private Float netSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PayrollStatus status = PayrollStatus.GENERATED;

    @Column(name = "payment_date")
    private LocalDate paymentDate;
}