package com.school.management.controller;


import com.school.management.dto.request.CreateAnnouncementRequest;
import com.school.management.dto.request.UpdateAnnouncementRequest;
import com.school.management.dto.response.AnnouncementResponse;
import com.school.management.dto.response.ApiResponse;
import com.school.management.entity.Announcement;
import com.school.management.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequestMapping("/announcements")
@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping("/")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request
            ) {
        List<AnnouncementResponse> announcements = announcementService.createAnnouncement(request);
        return ResponseEntity.ok(ApiResponse.success("Announcement Created Successfully ",announcements));
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getAnnouncements() {
        List<AnnouncementResponse> announcements = announcementService.getAnnouncements();
        return ResponseEntity.ok(ApiResponse.success("Announcement fetched Successfully ",announcements));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse announcementResponse = announcementService.getAnnouncementById(id);
        return ResponseEntity.ok(ApiResponse.success("Announcement fetched Successfully ",announcementResponse));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAnnouncementRequest updateAnnouncementRequest) {
        AnnouncementResponse announcementResponse = announcementService.updateAnnouncement(id, updateAnnouncementRequest);
        return ResponseEntity.ok(ApiResponse.success("Announcement Updated Successfully ",announcementResponse));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse announcementResponse = announcementService.getAnnouncementById(id);
        return ResponseEntity.noContent().build();

    }


}
