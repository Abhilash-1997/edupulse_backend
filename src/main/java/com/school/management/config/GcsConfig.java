package com.school.management.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;

@Configuration
public class GcsConfig {

    @Value("${GCS_CREDENTIALS_BASE64}")
    private String credentialsBase64;

    @Bean
    public Storage googleCloudStorage() throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(credentialsBase64);

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(decodedBytes))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }
}