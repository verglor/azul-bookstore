package com.verglor.azul.bookstore.service;

import com.verglor.azul.bookstore.domain.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Genre entity operations
 * Defines business logic methods for genre management
 */
public interface GenreService {

    /**
     * Get all genres with pagination and optional name filtering
     */
    Page<Genre> getAllGenres(String name, Pageable pageable);

    /**
     * Get genre by ID
     */
    Genre getGenreById(Long genreId);

    /**
     * Create a new genre
     */
    Genre createGenre(String name);

    /**
     * Update an existing genre
     */
    Genre updateGenre(Long genreId, String name);

    /**
     * Delete a genre by ID
     */
    void deleteGenre(Long genreId);
}