package com.school.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGalleryRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private LocalDate eventDate;
}