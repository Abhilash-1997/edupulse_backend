package com.school.management.dto.request;

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
public class CreateSectionRequest {

    @NotNull(message = "Class ID is required")
    private UUID classId;

    @NotBlank(message = "Section name is required")
    private String name;

    private UUID classTeacherId;
}