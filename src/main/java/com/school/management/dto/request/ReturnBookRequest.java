package com.school.management.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class ReturnBookRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    private LocalDateTime returnDate;
    private BigDecimal fineAmount;
    private String remarks;
}