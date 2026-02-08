package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private Boolean success;
    private String message;
    private String error;
    private LocalDateTime timestamp;
    private String path;

    public static ErrorResponse of(String message, String error, String path) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}