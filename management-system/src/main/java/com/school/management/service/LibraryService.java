package com.school.management.service;

import com.school.management.constant.LibraryTransactionStatus;
import com.school.management.dto.request.*;
import com.school.management.dto.response.*;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final LibrarySectionRepository librarySectionRepository;
    private final BookRepository bookRepository;
    private final LibraryTransactionRepository libraryTransactionRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;

    // ============= SECTION MANAGEMENT =============

    @Transactional
    public LibrarySectionResponse createSection(CreateLibrarySectionRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        LibrarySection section = LibrarySection.builder()
                .school(school)
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
                .build();

        section = librarySectionRepository.save(section);

        return mapToLibrarySectionResponse(section);
    }

    @Transactional(readOnly = true)
    public List<LibrarySectionResponse> getSections() {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<LibrarySection> sections = librarySectionRepository
                .findBySchool_IdAndDeletedAtIsNull(schoolId);

        return sections.stream()
                .map(this::mapToLibrarySectionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LibrarySectionResponse updateSection(UUID id, CreateLibrarySectionRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        LibrarySection section = librarySectionRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        section.setName(request.getName());
        section.setDescription(request.getDescription());
        section.setLocation(request.getLocation());

        section = librarySectionRepository.save(section);

        return mapToLibrarySectionResponse(section);
    }

    @Transactional
    public void deleteSection(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        LibrarySection section = librarySectionRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        librarySectionRepository.delete(section);
    }

    // ============= BOOK MANAGEMENT =============

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        LibrarySection section = librarySectionRepository
                .findByIdAndSchool_IdAndDeletedAtIsNull(request.getSectionId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Library section not found"));

        Book book = Book.builder()
                .school(school)
                .section(section)
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .publisher(request.getPublisher())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .available(request.getQuantity()) // Initially all available
                .description(request.getDescription())
                .build();

        book = bookRepository.save(book);

        return mapToBookResponse(book);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getBooks(UUID sectionId, String search) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Book> books;

        if (search != null && !search.isEmpty()) {
            books = bookRepository.searchBooks(schoolId, search);
        } else if (sectionId != null) {
            books = bookRepository.findBySchool_IdAndSection_IdAndDeletedAtIsNull(
                    schoolId, sectionId);
        } else {
            books = bookRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        }

        return books.stream()
                .map(this::mapToBookResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookResponse getBookDetails(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Book book = bookRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        return mapToBookResponse(book);
    }

    @Transactional
    public BookResponse updateBook(UUID id, UpdateBookRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Book book = bookRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        // Adjust available count if quantity changes
        if (request.getQuantity() != null) {
            int diff = request.getQuantity() - book.getQuantity();
            book.setAvailable(book.getAvailable() + diff);
            book.setQuantity(request.getQuantity());
        }

        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getIsbn() != null) {
            book.setIsbn(request.getIsbn());
        }
        if (request.getPublisher() != null) {
            book.setPublisher(request.getPublisher());
        }
        if (request.getCategory() != null) {
            book.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }

        book = bookRepository.save(book);

        return mapToBookResponse(book);
    }

    @Transactional
    public void deleteBook(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Book book = bookRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        bookRepository.delete(book);
    }

    // ============= TRANSACTION MANAGEMENT =============

    @Transactional
    public LibraryTransactionResponse issueBook(IssueBookRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        if (request.getUserId() == null && request.getStudentId() == null) {
            throw new BadRequestException("User or Student is required");
        }

        // Check book availability
        Book book = bookRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                        request.getBookId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (book.getAvailable() < 1) {
            throw new BadRequestException("Book not available");
        }

        LibraryTransaction transaction = LibraryTransaction.builder()
                .school(school)
                .book(book)
                .issueDate(LocalDateTime.now())
                .dueDate(request.getDueDate())
                .status(LibraryTransactionStatus.ISSUED)
                .fineAmount(BigDecimal.ZERO)
                .build();

        if (request.getUserId() != null) {
            User user = new User();
            user.setId(request.getUserId());
            transaction.setUser(user);
        }

        if (request.getStudentId() != null) {
            Student student = new Student();
            student.setId(request.getStudentId());
            transaction.setStudent(student);
        }

        transaction = libraryTransactionRepository.save(transaction);

        // Decrement available count
        book.setAvailable(book.getAvailable() - 1);
        bookRepository.save(book);

        return mapToLibraryTransactionResponse(transaction);
    }

    @Transactional
    public void returnBook(ReturnBookRequest request) {
        LibraryTransaction transaction = libraryTransactionRepository
                .findByIdAndDeletedAtIsNull(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (transaction.getStatus() == LibraryTransactionStatus.RETURNED) {
            throw new BadRequestException("Book already returned");
        }

        transaction.setReturnDate(request.getReturnDate() != null ?
                request.getReturnDate() : LocalDateTime.now());
        transaction.setStatus(LibraryTransactionStatus.RETURNED);
        transaction.setFineAmount(request.getFineAmount() != null ?
                request.getFineAmount() : BigDecimal.ZERO);
        transaction.setRemarks(request.getRemarks());

        libraryTransactionRepository.save(transaction);

        // Increment book availability
        Book book = transaction.getBook();
        book.setAvailable(book.getAvailable() + 1);
        bookRepository.save(book);
    }

    @Transactional
    public void renewBook(RenewBookRequest request) {
        LibraryTransaction transaction = libraryTransactionRepository
                .findByIdAndDeletedAtIsNull(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (transaction.getStatus() != LibraryTransactionStatus.ISSUED) {
            throw new BadRequestException("Invalid transaction status");
        }

        transaction.setDueDate(request.getNewDueDate());
        libraryTransactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<LibraryTransactionResponse> getTransactions(UUID userId,
                                                            LibraryTransactionStatus status) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<LibraryTransaction> transactions;

        if (userId != null && status != null) {
            transactions = libraryTransactionRepository
                    .findBySchool_IdAndUser_IdAndDeletedAtIsNull(schoolId, userId);
            transactions = transactions.stream()
                    .filter(t -> t.getStatus() == status)
                    .collect(Collectors.toList());
        } else if (userId != null) {
            transactions = libraryTransactionRepository
                    .findBySchool_IdAndUser_IdAndDeletedAtIsNull(schoolId, userId);
        } else if (status != null) {
            transactions = libraryTransactionRepository
                    .findBySchool_IdAndStatusAndDeletedAtIsNull(schoolId, status);
        } else {
            transactions = libraryTransactionRepository
                    .findBySchool_IdAndDeletedAtIsNull(schoolId);
        }

        return transactions.stream()
                .map(this::mapToLibraryTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LibraryDashboardStatsResponse getDashboardStats() {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Long totalBooks = bookRepository.countBySchool_IdAndDeletedAtIsNull(schoolId);
        Long totalSections = librarySectionRepository.countBySchool_IdAndDeletedAtIsNull(schoolId);
        Long issuedBooks = libraryTransactionRepository
                .countBySchool_IdAndStatusAndDeletedAtIsNull(
                        schoolId, LibraryTransactionStatus.ISSUED);
        Long overdueBooks = libraryTransactionRepository
                .countOverdueBooks(schoolId, LocalDateTime.now());

        return LibraryDashboardStatsResponse.builder()
                .totalBooks(totalBooks)
                .totalSections(totalSections)
                .issuedBooks(issuedBooks)
                .overdueBooks(overdueBooks)
                .build();
    }

    // ============= MAPPERS =============

    private LibrarySectionResponse mapToLibrarySectionResponse(LibrarySection section) {
        return LibrarySectionResponse.builder()
                .id(section.getId())
                .name(section.getName())
                .description(section.getDescription())
                .location(section.getLocation())
                .build();
    }

    private BookResponse mapToBookResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .category(book.getCategory())
                .quantity(book.getQuantity())
                .available(book.getAvailable())
                .description(book.getDescription())
                .sectionId(book.getSection().getId())
                .sectionName(book.getSection().getName())
                .build();
    }

    private LibraryTransactionResponse mapToLibraryTransactionResponse(
            LibraryTransaction transaction) {
        return LibraryTransactionResponse.builder()
                .id(transaction.getId())
                .issueDate(transaction.getIssueDate())
                .dueDate(transaction.getDueDate())
                .returnDate(transaction.getReturnDate())
                .status(transaction.getStatus())
                .fineAmount(transaction.getFineAmount())
                .remarks(transaction.getRemarks())
                .bookId(transaction.getBook().getId())
                .bookTitle(transaction.getBook().getTitle())
                .bookIsbn(transaction.getBook().getIsbn())
                .userId(transaction.getUser() != null ? transaction.getUser().getId() : null)
                .studentId(transaction.getStudent() != null ?
                        transaction.getStudent().getId() : null)
                .build();
    }
}