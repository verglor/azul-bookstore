package com.verglor.azul.bookstore.service;

import com.verglor.azul.bookstore.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Book entity operations
 * Defines business logic methods for book management
 */
public interface BookService {

    /**
     * Get all books with pagination and optional filtering
     */
    Page<Book> getAllBooks(String title, String author, String genre, Pageable pageable);

    /**
     * Get book by ID
     */
    Book getBookById(Long bookId);

    /**
     * Create a new book
     */
    Book createBook(String title, java.math.BigDecimal price, java.util.List<Long> authorIds, java.util.List<Long> genreIds);

    /**
     * Update an existing book
     */
    Book updateBook(Long bookId, String title, java.math.BigDecimal price, java.util.List<Long> authorIds, java.util.List<Long> genreIds);

    /**
     * Delete a book by ID
     */
    void deleteBook(Long bookId);
}