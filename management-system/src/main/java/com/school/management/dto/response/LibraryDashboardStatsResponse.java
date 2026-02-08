package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryDashboardStatsResponse {

    private Long totalBooks;
    private Long totalSections;
    private Long issuedBooks;
    private Long overdueBooks;
}