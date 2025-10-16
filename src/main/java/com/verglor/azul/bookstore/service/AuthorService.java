package com.verglor.azul.bookstore.service;

import com.verglor.azul.bookstore.domain.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Author entity operations
 * Defines business logic methods for author management
 */
public interface AuthorService {

    /**
     * Get all authors with pagination and optional name filtering
     */
    Page<Author> getAllAuthors(String name, Pageable pageable);

    /**
     * Get author by ID
     */
    Author getAuthorById(Long authorId);

    /**
     * Create a new author
     */
    Author createAuthor(String name);

    /**
     * Update an existing author
     */
    Author updateAuthor(Long authorId, String name);

    /**
     * Delete an author by ID
     */
    void deleteAuthor(Long authorId);
}