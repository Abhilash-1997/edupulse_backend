package com.school.management.controller;

import com.school.management.constant.LibraryTransactionStatus;
import com.school.management.dto.request.*;
import com.school.management.dto.response.*;
import com.school.management.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('LIBRARIAN', 'SCHOOL_ADMIN')")
public class LibraryController {

    private final LibraryService libraryService;

    @PostMapping("/sections")
    public ResponseEntity<ApiResponse<LibrarySectionResponse>> createSection(
            @Valid @RequestBody CreateLibrarySectionRequest request) {
        LibrarySectionResponse librarySectionResponse = libraryService.createSection(request);
        return ResponseEntity.ok(ApiResponse.success("library Section created successfully", librarySectionResponse));
    }

    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<List<LibrarySectionResponse>>> getSections() {
        List<LibrarySectionResponse> librarySectionResponse = libraryService.getSections();
        return ResponseEntity.ok(ApiResponse.success("library Section created successfully", librarySectionResponse));
    }

    @PutMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<LibrarySectionResponse>> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody CreateLibrarySectionRequest request) {
        LibrarySectionResponse librarySectionResponse = libraryService.updateSection(id, request);
        return ResponseEntity.ok(ApiResponse.success("library Section created successfully", librarySectionResponse));
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID id) {
        libraryService.deleteSection(id);
        return ResponseEntity.ok(ApiResponse.success("library Section deleted successfully", null));
    }

    @PostMapping("/books")
    public ResponseEntity<ApiResponse<BookResponse>> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponse bookResponse = libraryService.createBook(request);
        return ResponseEntity.ok(ApiResponse.success("library Book created successfully", bookResponse));
    }

    @GetMapping("/books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getBooks(
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) String search) {
        List<BookResponse> response = libraryService.getBooks(sectionId, search);
        return ResponseEntity.ok(ApiResponse.success("library Book fetched successfully", response));
    }

    @GetMapping("/section-books/{sectionId}")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getLibraryBook(@PathVariable UUID sectionId) {
        List<BookResponse> response = libraryService.getLibraryBook(sectionId);
        return ResponseEntity.ok(ApiResponse.success("Library books by section fetched successfully", response));
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<ApiResponse<BookResponse>> getBookDetails(@PathVariable UUID id) {
        BookResponse bookResponse = libraryService.getBookDetails(id);
        return ResponseEntity.ok(ApiResponse.success("library Book details fetched successfully", bookResponse));
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBookRequest request) {
        BookResponse bookResponse = libraryService.updateBook(id, request);
        return ResponseEntity.ok(ApiResponse.success("library Book Updated successfully", bookResponse));
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable UUID id) {
        libraryService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success("library Book Deleted successfully", null));
    }

    // ====================== Library Book Transaction Routes
    // =============================================

    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<LibraryTransactionResponse>> issueBook(
            @Valid @RequestBody IssueBookRequest request) {
        LibraryTransactionResponse libraryTransactionResponse = libraryService.issueBook(request);
        return ResponseEntity.ok(ApiResponse.success("library Book Issued successfully", libraryTransactionResponse));
    }

    @PostMapping("/return")
    public ResponseEntity<ApiResponse<Void>> returnBook(@Valid @RequestBody ReturnBookRequest request) {
        libraryService.returnBook(request);
        return ResponseEntity.ok(ApiResponse.success("library Book Returned successfully", null));
    }

    @PostMapping("/renew")
    public ResponseEntity<ApiResponse<Void>> renewBook(@Valid @RequestBody RenewBookRequest request) {
        libraryService.renewBook(request);
        return ResponseEntity.ok(ApiResponse.success("library Book Renewed successfully", null));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<LibraryTransactionResponse>>> getTransactions(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) LibraryTransactionStatus status) {
        List<LibraryTransactionResponse> libraryTransactionResponse = libraryService.getTransactions(userId, status);
        return ResponseEntity.ok(ApiResponse.success("Library Transactions", libraryTransactionResponse));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<LibraryDashboardStatsResponse>> getStats() {
        LibraryDashboardStatsResponse libraryDashboardStatsResponse = libraryService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Library Dashboard Stats", libraryDashboardStatsResponse));
    }

}
