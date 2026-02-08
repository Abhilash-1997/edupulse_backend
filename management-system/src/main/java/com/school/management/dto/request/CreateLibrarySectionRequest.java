package com.school.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLibrarySectionRequest {

    @NotBlank(message = "Section name is required")
    private String name;

    private String description;
    private String location;
}