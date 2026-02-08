package com.school.management.dto.request;

import com.school.management.constant.FeeFrequency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class CreateFeeStructureRequest {

    @NotNull(message = "Class ID is required")
    private UUID classId;

    @NotBlank(message = "Fee name is required")
    private String name;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount cannot be negative")
    private Float amount;

    @NotNull(message = "Frequency is required")
    private FeeFrequency frequency;

    private String dueDate;
}