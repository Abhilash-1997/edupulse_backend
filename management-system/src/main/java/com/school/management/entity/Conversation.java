package com.school.management.entity;

import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversations_school_users",
                        columnNames = {"school_id", "user_a_id", "user_b_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_conversations_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id", nullable = false, foreignKey = @ForeignKey(name = "fk_conversations_user_a"))
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id", nullable = false, foreignKey = @ForeignKey(name = "fk_conversations_user_b"))
    private User userB;

    @Column(name = "last_message_at")
    @Builder.Default
    private LocalDateTime lastMessageAt = LocalDateTime.now();
}