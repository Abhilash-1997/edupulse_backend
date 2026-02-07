package com.school.management.controller;

import com.school.management.dto.request.CreateParentRequest;
import com.school.management.dto.response.ApiResponse;
import com.school.management.dto.response.MyChildrenResponse;
import com.school.management.dto.response.ParentResponse;
import com.school.management.service.ParentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/parents")
@RestController
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ParentResponse>> createParent(@Valid @RequestBody CreateParentRequest createParentRequest) {
        ParentResponse parentResponse = parentService.createParent(createParentRequest);
        return ResponseEntity.ok(ApiResponse.success(parentResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParentResponse>>> getAllParents() {
        List<ParentResponse> parentResponseList = parentService.getAllParents();
        return ResponseEntity.ok(ApiResponse.success(parentResponseList));
    }

    @GetMapping("/children")
    public ResponseEntity<ApiResponse<MyChildrenResponse>> getMyChildren() {
        MyChildrenResponse myChildrenResponse = parentService.getMyChildren();
        return ResponseEntity.ok(ApiResponse.success(myChildrenResponse));
    }
}
