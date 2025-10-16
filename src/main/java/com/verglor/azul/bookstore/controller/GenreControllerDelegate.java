package com.verglor.azul.bookstore.controller;

import com.verglor.azul.bookstore.domain.Genre;
import com.verglor.azul.bookstore.openapi.api.GenresApiDelegate;
import com.verglor.azul.bookstore.openapi.model.GenreRequest;
import com.verglor.azul.bookstore.openapi.model.GenreResponse;
import com.verglor.azul.bookstore.openapi.model.PagedResponse;
import com.verglor.azul.bookstore.openapi.model.PagedResponseContentInner;
import com.verglor.azul.bookstore.openapi.model.PageInfo;
import com.verglor.azul.bookstore.service.GenreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for Genre entity operations
 * Provides comprehensive CRUD functionality with proper error handling and validation
 * Implements the generated GenresApiDelegate interface for OpenAPI compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenreControllerDelegate implements GenresApiDelegate {

    private final GenreService genreService;

    /**
     * Get all genres with pagination and sorting support
     * Implements GenresApiDelegate.getAllGenres method
     * Uses Spring Boot's automatic Pageable parameter binding for seamless OpenAPI integration
     */
    @Override
    public ResponseEntity<PagedResponse> getAllGenres(String name, Pageable pageable) {

        log.debug("Fetching genres - name: {}, pageable: {}", name, pageable);

        Page<Genre> genres = genreService.getAllGenres(name, pageable);

        PagedResponse response = convertToPagedResponse(genres);

        log.info("Successfully retrieved {} genres", genres.getNumberOfElements());
        return ResponseEntity.ok(response);
    }


    /**
     * Get genre by ID
     * Implements GenresApiDelegate.getGenreById method
     */
    @Override
    public ResponseEntity<GenreResponse> getGenreById(Long genreId) {
        log.debug("Fetching genre with ID: {}", genreId);

        Genre genre = genreService.getGenreById(genreId);
        GenreResponse response = convertToResponse(genre);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new genre
     * Implements GenresApiDelegate.createGenre method
     */
    @Override
    public ResponseEntity<GenreResponse> createGenre(GenreRequest genreRequest) {
        log.debug("Creating new genre: {}", genreRequest.getName());

        Genre savedGenre = genreService.createGenre(genreRequest.getName());
        GenreResponse response = convertToResponse(savedGenre);

        log.info("Successfully created genre: {} with ID: {}", savedGenre.getName(), savedGenre.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing genre
     * Implements GenresApiDelegate.updateGenre method
     */
    @Override
    public ResponseEntity<GenreResponse> updateGenre(Long genreId, GenreRequest genreRequest) {

        log.debug("Updating genre with ID: {} - new name: {}", genreId, genreRequest.getName());

        Genre updatedGenre = genreService.updateGenre(genreId, genreRequest.getName());
        GenreResponse response = convertToResponse(updatedGenre);

        log.info("Successfully updated genre: {} with ID: {}", updatedGenre.getName(), updatedGenre.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a genre by ID
     * Implements GenresApiDelegate.deleteGenre method
     */
    @Override
    public ResponseEntity<Void> deleteGenre(Long genreId) {
        log.debug("Deleting genre with ID: {}", genreId);

        genreService.deleteGenre(genreId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convert Genre entity to GenreResponse DTO
     */
    private GenreResponse convertToResponse(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getName())
                .build();
    }


    /**
     * Convert Page<Genre> to PagedResponse for interface compliance
     */
    private PagedResponse convertToPagedResponse(Page<Genre> genres) {
        @SuppressWarnings("unchecked")
        List<PagedResponseContentInner> content = (List<PagedResponseContentInner>) (List<?>) genres.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        PageInfo pageInfo = PageInfo.builder()
                .size(genres.getSize())
                .totalElements(genres.getTotalElements())
                .totalPages(genres.getTotalPages())
                .number(genres.getNumber())
                .build();

        return PagedResponse.builder()
                .content(content)
                .page(pageInfo)
                .build();
    }
}