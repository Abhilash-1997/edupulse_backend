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
public class RenewBookRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    @NotNull(message = "New due date is required")
    private LocalDateTime newDueDate;
}