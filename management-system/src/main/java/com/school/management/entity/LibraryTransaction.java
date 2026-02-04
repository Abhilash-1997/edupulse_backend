package com.school.management.entity;

import com.school.management.constant.LibraryTransactionStatus;
import com.school.management.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "library_transactions",
        indexes = {
                @Index(name = "idx_library_transactions_school", columnList = "school_id"),
                @Index(name = "idx_library_transactions_book", columnList = "book_id"),
                @Index(name = "idx_library_transactions_user", columnList = "user_id"),
                @Index(name = "idx_library_transactions_student", columnList = "student_id"),
                @Index(name = "idx_library_transactions_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryTransaction extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, foreignKey = @ForeignKey(name = "fk_library_transactions_school"))
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_library_transactions_book"))
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_library_transactions_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", foreignKey = @ForeignKey(name = "fk_library_transactions_student"))
    private Student student;

    @Column(name = "issue_date", nullable = false)
    @Builder.Default
    private LocalDateTime issueDate = LocalDateTime.now();

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LibraryTransactionStatus status = LibraryTransactionStatus.ISSUED;

    @Column(name = "fine_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "remarks")
    private String remarks;
}