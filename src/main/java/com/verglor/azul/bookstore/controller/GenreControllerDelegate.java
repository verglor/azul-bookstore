package com.verglor.azul.bookstore.controller;

import com.verglor.azul.bookstore.domain.Genre;
import com.verglor.azul.bookstore.exception.ConflictException;
import com.verglor.azul.bookstore.exception.NotFoundException;
import com.verglor.azul.bookstore.openapi.api.GenresApiDelegate;
import com.verglor.azul.bookstore.openapi.model.GenreRequest;
import com.verglor.azul.bookstore.openapi.model.GenreResponse;
import com.verglor.azul.bookstore.openapi.model.PagedResponse;
import com.verglor.azul.bookstore.openapi.model.PagedResponseContentInner;
import com.verglor.azul.bookstore.openapi.model.PageInfo;
import com.verglor.azul.bookstore.repository.GenreRepository;
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

    private final GenreRepository genreRepository;

    /**
     * Get all genres with pagination and sorting support
     * Implements GenresApiDelegate.getAllGenres method
     * Uses Spring Boot's automatic Pageable parameter binding for seamless OpenAPI integration
     */
    @Override
    public ResponseEntity<PagedResponse> getAllGenres(String name, Pageable pageable) {

        log.debug("Fetching genres - name: {}, pageable: {}", name, pageable);

        Page<Genre> genres;
        if (name != null && !name.trim().isEmpty()) {
            genres = genreRepository.findByNameIgnoreCaseContaining(name.trim(), pageable);
        } else {
            genres = genreRepository.findAll(pageable);
        }

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

        Optional<Genre> genreOpt = genreRepository.findById(genreId);

        if (genreOpt.isPresent()) {
            GenreResponse response = convertToResponse(genreOpt.get());
            log.info("Successfully retrieved genre: {}", genreOpt.get().getName());
            return ResponseEntity.ok(response);
        } else {
            throw new NotFoundException("Genre not found with ID: " + genreId);
        }
    }

    /**
     * Create a new genre
     * Implements GenresApiDelegate.createGenre method
     */
    @Override
    public ResponseEntity<GenreResponse> createGenre(GenreRequest genreRequest) {
        log.debug("Creating new genre: {}", genreRequest.getName());

        // Check if genre with same name already exists
        if (genreRepository.existsByNameIgnoreCase(genreRequest.getName())) {
            throw new ConflictException("Genre already exists with name: " + genreRequest.getName());
        }

        Genre genre = Genre.builder()
                .name(genreRequest.getName())
                .build();

        Genre savedGenre = genreRepository.save(genre);
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

        Optional<Genre> genreOpt = genreRepository.findById(genreId);

        if (genreOpt.isPresent()) {
            Genre genre = genreOpt.get();

            // Check if another genre with same name exists
            Optional<Genre> existingGenre = genreRepository.findByNameIgnoreCase(genreRequest.getName());
            if (existingGenre.isPresent() && !existingGenre.get().getId().equals(genreId)) {
                throw new ConflictException("Another genre already exists with name: " + genreRequest.getName());
            }

            genre.setName(genreRequest.getName());
            Genre updatedGenre = genreRepository.save(genre);
            GenreResponse response = convertToResponse(updatedGenre);

            log.info("Successfully updated genre: {} with ID: {}", updatedGenre.getName(), updatedGenre.getId());
            return ResponseEntity.ok(response);

        } else {
            throw new NotFoundException("Genre not found with ID: " + genreId);
        }
    }

    /**
     * Delete a genre by ID
     * Implements GenresApiDelegate.deleteGenre method
     */
    @Override
    public ResponseEntity<Void> deleteGenre(Long genreId) {
        log.debug("Deleting genre with ID: {}", genreId);

        Optional<Genre> genreOpt = genreRepository.findById(genreId);

        if (genreOpt.isPresent()) {
            Genre genre = genreOpt.get();

            // Check if genre has associated books
            if (!genre.getBooks().isEmpty()) {
                throw new ConflictException("Cannot delete genre " + genre.getName() + " - has associated books");
            }

            genreRepository.deleteById(genreId);
            log.info("Successfully deleted genre: {} with ID: {}", genre.getName(), genreId);
            return ResponseEntity.noContent().build();

        } else {
            throw new NotFoundException("Genre not found with ID: " + genreId);
        }
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