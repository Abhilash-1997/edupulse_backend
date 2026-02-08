package com.school.management.dto.response;

import com.school.management.constant.FeeFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureResponse {

    private UUID id;
    private String name;
    private Float amount;
    private FeeFrequency frequency;
    private String dueDate;
    private UUID classId;
}