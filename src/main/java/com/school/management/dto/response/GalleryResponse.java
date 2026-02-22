package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryResponse {

    private UUID id;
    private String title;
    private String description;
    private LocalDate eventDate;
    private List<GalleryImageResponse> images;
}