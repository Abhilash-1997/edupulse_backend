package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamTokenResponse {

    private String streamToken;
    private String streamUrl;
    private MaterialInfo material;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialInfo {
        private String id;
        private String title;
        private String description;
        private Integer duration;
    }
}