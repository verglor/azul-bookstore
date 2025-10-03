package com.verglor.azul.bookstore.repository;

import com.verglor.azul.bookstore.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:authorName IS NULL OR EXISTS (SELECT a FROM b.authors a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%')))) AND " +
           "(:genreName IS NULL OR EXISTS (SELECT g FROM b.genres g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :genreName, '%'))))")
    Page<Book> findBooksWithFilters(
        @Param("title") String title,
        @Param("authorName") String authorName,
        @Param("genreName") String genreName,
        Pageable pageable);

}