package com.school.management.repository;

import com.school.management.entity.Book;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends BaseRepository<Book> {

    Optional<Book> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Book> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    Optional<Book> findBySchool_IdAndIsbnAndDeletedAtIsNull(UUID schoolId, String isbn);

    List<Book> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<Book> findBySchool_IdAndSection_IdAndDeletedAtIsNull(UUID schoolId, UUID sectionId);

    @Query("SELECT b FROM Book b WHERE b.school.id = :schoolId " +
            "AND (b.title LIKE %:search% OR b.author LIKE %:search% OR b.isbn LIKE %:search%) " +
            "AND b.deletedAt IS NULL")
    List<Book> searchBooks(
            @Param("schoolId") UUID schoolId,
            @Param("search") String search
    );

    long countBySchool_IdAndDeletedAtIsNull(UUID schoolId);
}