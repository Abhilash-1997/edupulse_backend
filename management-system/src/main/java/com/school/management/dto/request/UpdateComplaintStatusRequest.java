package com.school.management.dto.request;

import com.school.management.constant.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplaintStatusRequest {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;
}
