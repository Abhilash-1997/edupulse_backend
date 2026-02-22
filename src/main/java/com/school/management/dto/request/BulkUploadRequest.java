package com.school.management.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// File will be handled via MultipartFile in controller
@Data
@Builder
@NoArgsConstructor
public class BulkUploadRequest {

    // This is a marker DTO - actual file comes from MultipartFile
    // Column mapping documented in controller for each upload type
}