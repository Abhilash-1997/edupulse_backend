package com.school.management.service;

import com.school.management.constant.StudyMaterialStatus;
import com.school.management.repository.StudyMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles asynchronous video processing:
 * 1. Downloads original video from GCS
 * 2. Converts to HLS format using FFmpeg
 * 3. Uploads HLS segments back to GCS
 * 4. Updates material record with hlsPath, duration, and status
 *
 * Mirrors the Node.js processVideo() + convertToHLS() pipeline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private final GcsService gcsService;
    private final StudyMaterialRepository studyMaterialRepository;

    @Value("${video.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${video.ffprobe-path:ffprobe}")
    private String ffprobePath;

    @Value("${video.segment-duration:6}")
    private int segmentDuration;

    @Value("${video.process-timeout-minutes:30}")
    private int processTimeoutMinutes;

    /**
     * Async entry point — called after the material is saved with PROCESSING
     * status.
     * This method runs in a separate thread and will not block the HTTP response.
     */
    @Async
    public void processVideoAsync(UUID materialId, String gcsInputPath, UUID sectionId) {
        Path tempDir = null;

        try {
            log.info("Starting video processing for material: {}", materialId);

            // 1. Create temp working directory
            tempDir = Files.createTempDirectory("video-processing-" + materialId);
            Path inputFile = tempDir.resolve("input" + getExtension(gcsInputPath));
            Path hlsOutputDir = tempDir.resolve("hls");
            Files.createDirectories(hlsOutputDir);

            // 2. Download original video from GCS
            log.info("Downloading video from GCS: {}", gcsInputPath);
            gcsService.downloadFile(gcsInputPath, inputFile);

            // 3. Convert to HLS using FFmpeg
            log.info("Converting to HLS format...");
            HlsResult hlsResult = convertToHLS(inputFile, hlsOutputDir);
            log.info("HLS conversion completed. Duration: {}s, Output: {}", hlsResult.duration, hlsOutputDir);

            // 4. Upload all HLS files to GCS
            String gcsHlsFolder = "study-materials/" + sectionId + "/" + materialId + "/hls";
            String masterManifestGcsPath = uploadHlsFilesToGcs(hlsOutputDir, gcsHlsFolder);
            log.info("HLS files uploaded to GCS: {}", gcsHlsFolder);

            // 5. Update material record
            updateMaterialSuccess(materialId, masterManifestGcsPath, hlsResult.duration);
            log.info("Video processing completed successfully for material: {}", materialId);

        } catch (Exception e) {
            log.error("Video processing failed for material {}: {}", materialId, e.getMessage(), e);
            updateMaterialFailed(materialId);
        } finally {
            // Always clean up temp directory
            cleanupTempDir(tempDir);
        }
    }

    /**
     * Convert video to HLS format using FFmpeg.
     * Mirrors the Node.js convertToHLS() function.
     */
    private HlsResult convertToHLS(Path inputFile, Path outputDir) throws IOException, InterruptedException {
        Path masterManifest = outputDir.resolve("master.m3u8");

        // Build FFmpeg command — same args as the Node.js version
        ProcessBuilder ffmpegProcess = new ProcessBuilder(
                ffmpegPath,
                "-i", inputFile.toString(),
                "-profile:v", "baseline",
                "-level", "3.0",
                "-start_number", "0",
                "-hls_time", String.valueOf(segmentDuration),
                "-hls_list_size", "0",
                "-hls_segment_filename", outputDir.resolve("segment_%03d.ts").toString(),
                "-f", "hls",
                masterManifest.toString());
        ffmpegProcess.redirectErrorStream(true);

        Process process = ffmpegProcess.start();

        // Capture output for logging
        StringBuilder processOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processOutput.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(processTimeoutMinutes, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("FFmpeg process timed out after " + processTimeoutMinutes + " minutes");
        }

        if (process.exitValue() != 0) {
            log.error("FFmpeg output: {}", processOutput);
            throw new IOException("FFmpeg failed with exit code: " + process.exitValue());
        }

        // Extract duration using FFprobe (optional — same as Node.js)
        Integer duration = extractDuration(inputFile);

        return new HlsResult(masterManifest, duration);
    }

    /**
     * Extract video duration using FFprobe.
     * Returns null if FFprobe is unavailable (non-fatal).
     */
    private Integer extractDuration(Path inputFile) {
        try {
            ProcessBuilder ffprobeProcess = new ProcessBuilder(
                    ffprobePath,
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    inputFile.toString());
            ffprobeProcess.redirectErrorStream(true);

            Process process = ffprobeProcess.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            if (!completed || process.exitValue() != 0) {
                log.warn("FFprobe failed or timed out, duration will be null");
                return null;
            }

            // Parse JSON output to extract duration
            // Format: { "format": { "duration": "123.456" } }
            String json = output.toString();
            int durationIdx = json.indexOf("\"duration\"");
            if (durationIdx != -1) {
                int colonIdx = json.indexOf(":", durationIdx);
                int quoteStart = json.indexOf("\"", colonIdx + 1);
                int quoteEnd = json.indexOf("\"", quoteStart + 1);
                if (quoteStart != -1 && quoteEnd != -1) {
                    String durationStr = json.substring(quoteStart + 1, quoteEnd);
                    return (int) Math.round(Double.parseDouble(durationStr));
                }
            }

            log.warn("Could not parse duration from FFprobe output");
            return null;

        } catch (Exception e) {
            log.warn("FFprobe not available or failed, duration will be null: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Upload all HLS files (.m3u8 and .ts) from local directory to GCS.
     * Returns the GCS path of the master manifest.
     */
    private String uploadHlsFilesToGcs(Path hlsDir, String gcsFolder) throws IOException {
        String masterManifestPath = null;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(hlsDir)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file))
                    continue;

                String fileName = file.getFileName().toString();
                String contentType;

                if (fileName.endsWith(".m3u8")) {
                    contentType = "application/vnd.apple.mpegurl";
                } else if (fileName.endsWith(".ts")) {
                    contentType = "video/mp2t";
                } else {
                    continue; // Skip unknown files
                }

                String gcsPath = gcsService.uploadFileFromPath(gcsFolder, file, contentType);

                if (fileName.equals("master.m3u8")) {
                    masterManifestPath = gcsPath;
                }
            }
        }

        if (masterManifestPath == null) {
            throw new IOException("master.m3u8 not found in HLS output");
        }

        return masterManifestPath;
    }

    @Transactional
    protected void updateMaterialSuccess(UUID materialId, String hlsPath, Integer duration) {
        studyMaterialRepository.findByIdAndDeletedAtIsNull(materialId).ifPresent(material -> {
            material.setHlsPath(hlsPath);
            material.setDuration(duration != null ? duration : 0);
            material.setStatus(StudyMaterialStatus.COMPLETED);
            studyMaterialRepository.save(material);
            log.info("Material {} updated: status=COMPLETED, hlsPath={}, duration={}",
                    materialId, hlsPath, duration);
        });
    }

    @Transactional
    protected void updateMaterialFailed(UUID materialId) {
        studyMaterialRepository.findByIdAndDeletedAtIsNull(materialId).ifPresent(material -> {
            material.setStatus(StudyMaterialStatus.FAILED);
            studyMaterialRepository.save(material);
            log.error("Material {} updated: status=FAILED", materialId);
        });
    }

    private void cleanupTempDir(Path tempDir) {
        if (tempDir == null)
            return;
        try {
            File dir = tempDir.toFile();
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            // Recurse into subdirectories (hls/)
                            File[] subFiles = f.listFiles();
                            if (subFiles != null) {
                                for (File sf : subFiles) {
                                    sf.delete();
                                }
                            }
                            f.delete();
                        } else {
                            f.delete();
                        }
                    }
                }
                dir.delete();
            }
            log.debug("Cleaned up temp directory: {}", tempDir);
        } catch (Exception e) {
            log.warn("Failed to cleanup temp directory {}: {}", tempDir, e.getMessage());
        }
    }

    private String getExtension(String path) {
        int dotIdx = path.lastIndexOf('.');
        return dotIdx != -1 ? path.substring(dotIdx) : "";
    }

    /**
     * Internal result holder for HLS conversion output.
     */
    private record HlsResult(Path manifestPath, Integer duration) {
    }
}
