package com.school.management.dto.response;

import com.school.management.constant.LibraryTransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryTransactionResponse {

    private UUID id;
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private LibraryTransactionStatus status;
    private BigDecimal fineAmount;
    private String remarks;
    private UUID bookId;
    private String bookTitle;
    private String bookIsbn;
    private UUID userId;
    private UUID studentId;
    private UserResponse user;
    private StudentResponse student;
}