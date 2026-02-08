package com.school.management.dto.response;

import com.school.management.constant.PaymentMethod;
import com.school.management.constant.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentResponse {

    private UUID id;
    private Float amountPaid;
    private LocalDate paymentDate;
    private String transactionId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private UUID studentId;
    private UUID feeStructureId;
    private String feeName;
}