package com.school.management.dto.request;

import com.school.management.constant.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLeaveStatusRequest {

    @NotNull(message = "Status is required")
    private LeaveStatus status;
}