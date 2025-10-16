package com.verglor.azul.bookstore.controller;

import com.verglor.azul.bookstore.domain.Author;
import com.verglor.azul.bookstore.openapi.api.AuthorsApiDelegate;
import com.verglor.azul.bookstore.openapi.model.AuthorRequest;
import com.verglor.azul.bookstore.openapi.model.AuthorResponse;
import com.verglor.azul.bookstore.openapi.model.PagedResponse;
import com.verglor.azul.bookstore.openapi.model.PagedResponseContentInner;
import com.verglor.azul.bookstore.openapi.model.PageInfo;
import com.verglor.azul.bookstore.service.AuthorService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for Author entity operations
 * Provides comprehensive CRUD functionality with proper error handling and validation
 * Implements the generated AuthorsApiDelegate interface for OpenAPI compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorControllerDelegate implements AuthorsApiDelegate {

    private final AuthorService authorService;

    /**
     * Get all authors with pagination and sorting support
     * Implements AuthorsApiDelegate.getAllAuthors method
     * Uses Spring Boot's automatic Pageable parameter binding for seamless OpenAPI integration
     */
    @Override
    public ResponseEntity<PagedResponse> getAllAuthors(String name, Pageable pageable) {

        log.debug("Fetching authors - name: {}, pageable: {}", name, pageable);

        Page<Author> authors = authorService.getAllAuthors(name, pageable);

        PagedResponse response = convertToPagedResponse(authors);

        log.info("Successfully retrieved {} authors", authors.getNumberOfElements());
        return ResponseEntity.ok(response);
    }


    /**
     * Get author by ID
     * Implements AuthorsApiDelegate.getAuthorById method
     */
    @Override
    public ResponseEntity<AuthorResponse> getAuthorById(Long authorId) {
        log.debug("Fetching author with ID: {}", authorId);

        Author author = authorService.getAuthorById(authorId);
        AuthorResponse response = convertToResponse(author);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new author
     * Implements AuthorsApiDelegate.createAuthor method
     */
    @Override
    public ResponseEntity<AuthorResponse> createAuthor(AuthorRequest authorRequest) {
        log.debug("Creating new author: {}", authorRequest.getName());

        Author savedAuthor = authorService.createAuthor(authorRequest.getName());
        AuthorResponse response = convertToResponse(savedAuthor);

        log.info("Successfully created author: {} with ID: {}", savedAuthor.getName(), savedAuthor.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing author
     * Implements AuthorsApiDelegate.updateAuthor method
     */
    @Override
    public ResponseEntity<AuthorResponse> updateAuthor(Long authorId, AuthorRequest authorRequest) {

        log.debug("Updating author with ID: {} - new name: {}", authorId, authorRequest.getName());

        Author updatedAuthor = authorService.updateAuthor(authorId, authorRequest.getName());
        AuthorResponse response = convertToResponse(updatedAuthor);

        log.info("Successfully updated author: {} with ID: {}", updatedAuthor.getName(), updatedAuthor.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an author by ID
     * Implements AuthorsApiDelegate.deleteAuthor method
     */
    @Override
    public ResponseEntity<Void> deleteAuthor(Long authorId) {
        log.debug("Deleting author with ID: {}", authorId);

        authorService.deleteAuthor(authorId);
        return ResponseEntity.noContent().build();
    }


    /**
     * Convert Author entity to AuthorResponse DTO
     */
    private AuthorResponse convertToResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .build();
    }


    /**
     * Convert Page<Author> to PagedResponse for interface compliance
     */
    private PagedResponse convertToPagedResponse(Page<Author> authors) {
        @SuppressWarnings("unchecked")
        List<PagedResponseContentInner> content = (List<PagedResponseContentInner>) (List<?>) authors.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        PageInfo pageInfo = PageInfo.builder()
                .size(authors.getSize())
                .totalElements(authors.getTotalElements())
                .totalPages(authors.getTotalPages())
                .number(authors.getNumber())
                .build();

        return PagedResponse.builder()
                .content(content)
                .page(pageInfo)
                .build();
    }
}