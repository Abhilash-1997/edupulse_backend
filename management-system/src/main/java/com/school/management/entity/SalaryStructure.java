package com.school.management.entity;

import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "salary_structures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructure extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_salary_structures_school"))
    private School school;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_salary_structures_staff"))
    private StaffProfile staff;

    @Column(name = "basic_salary", nullable = false)
    @Builder.Default
    private Float basicSalary = 0.0f;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowances", columnDefinition = "JSON")
    @Builder.Default
    private List<Map<String, Object>> allowances = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deductions", columnDefinition = "JSON")
    @Builder.Default
    private List<Map<String, Object>> deductions = new ArrayList<>();

    @Column(name = "net_salary")
    private Float netSalary;

    @Column(name = "effective_date", nullable = false)
    @Builder.Default
    private LocalDate effectiveDate = LocalDate.now();
}