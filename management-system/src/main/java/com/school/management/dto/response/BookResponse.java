package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private UUID id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String category;
    private Integer quantity;
    private Integer available;
    private String description;
    private UUID sectionId;
    private String sectionName;
}
