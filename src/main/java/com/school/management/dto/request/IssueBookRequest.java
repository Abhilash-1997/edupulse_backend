package com.school.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueBookRequest {

    @NotNull(message = "Book ID is required")
    private UUID bookId;

    // Either userId or studentId must be provided
    private UUID userId;
    private UUID studentId;

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;
}