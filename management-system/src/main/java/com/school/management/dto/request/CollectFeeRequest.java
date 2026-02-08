package com.school.management.dto.request;

import com.school.management.constant.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectFeeRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Fee structure ID is required")
    private UUID feeStructureId;

    @NotNull(message = "Amount paid is required")
    @Min(value = 0, message = "Amount cannot be negative")
    private Float amountPaid;

    // Alternative field name from Express
    private Float amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String transactionId;
}