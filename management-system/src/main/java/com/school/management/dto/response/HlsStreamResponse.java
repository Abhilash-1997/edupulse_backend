package com.school.management.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HlsStreamResponse {
    private String signedUrl;
    private String contentType;
    private String filename;
}