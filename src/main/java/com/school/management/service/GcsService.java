package com.school.management.service;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class GcsService {

    @Autowired
    private Storage storage;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    public String uploadFile(String folder, MultipartFile file) throws IOException {

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String objectName = folder + "/" + fileName;

        BlobId blobId = BlobId.of(bucketName, objectName);

        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                .build();

        storage.create(blobInfo, file.getBytes());

        return objectName;
    }

    /**
     * Upload a local file to GCS.
     * Used by VideoProcessingService to upload HLS segments and manifests.
     */
    public String uploadFileFromPath(String folder, Path localFile, String contentType) throws IOException {
        String fileName = localFile.getFileName().toString();
        String objectName = folder + "/" + fileName;

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        storage.create(blobInfo, Files.readAllBytes(localFile));

        return objectName;
    }

    /**
     * Download a GCS object to a local file path.
     * Used by VideoProcessingService to download the original video for FFmpeg
     * processing.
     */
    public void downloadFile(String objectName, Path destination) throws IOException {
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null) {
            throw new IOException("GCS object not found: " + objectName);
        }
        blob.downloadTo(destination);
    }

    public void deleteFile(String objectName) {
        storage.delete(bucketName, objectName);
    }

    public String generateSignedUrl(String objectName) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();
        URL url = storage.signUrl(blobInfo, 15, TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }

    public String generateSignedUrlWithExpiry(String objectName, int minutes) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();
        URL url = storage.signUrl(
                blobInfo,
                minutes,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }

    public byte[] downloadFileAsBytes(String objectPath) {
        Blob blob = storage.get(bucketName, objectPath);
        return blob.getContent();
    }
}
