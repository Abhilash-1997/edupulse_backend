package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponse {

    private Integer total;
    private Integer success;
    private Integer failed;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}