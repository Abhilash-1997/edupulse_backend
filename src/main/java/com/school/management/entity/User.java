package com.school.management.entity;

import com.school.management.constant.UserRole;
import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_school_email",
                        columnNames = {"school_id", "email"}
                )
        },
        indexes = {
                @Index(name = "idx_users_school_email", columnList = "school_id, email"),
                @Index(name = "idx_users_role", columnList = "role")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", foreignKey = @ForeignKey(name = "fk_users_school"))
    private School school;

    @Column(name = "name")
    private String name;

    @Email
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.STAFF;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}