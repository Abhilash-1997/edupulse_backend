package com.school.management.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HlsStreamResponse {
    private String gcsObjectPath;
//    private String signedUrl;  //for further Security
    private String contentType;
    private String filename;
}