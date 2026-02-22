package com.school.management.dto.request;

import com.school.management.constant.SchoolStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSchoolStatusRequest {
    @NotNull(message = "Status is required")
    private SchoolStatus status;
}
