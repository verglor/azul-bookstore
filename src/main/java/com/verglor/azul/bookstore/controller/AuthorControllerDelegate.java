package com.verglor.azul.bookstore.controller;

import com.verglor.azul.bookstore.domain.Author;
import com.verglor.azul.bookstore.exception.ConflictException;
import com.verglor.azul.bookstore.exception.NotFoundException;
import com.verglor.azul.bookstore.openapi.api.AuthorsApiDelegate;
import com.verglor.azul.bookstore.openapi.model.AuthorRequest;
import com.verglor.azul.bookstore.openapi.model.AuthorResponse;
import com.verglor.azul.bookstore.openapi.model.PagedResponse;
import com.verglor.azul.bookstore.openapi.model.PagedResponseContentInner;
import com.verglor.azul.bookstore.openapi.model.PageInfo;
import com.verglor.azul.bookstore.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for Author entity operations
 * Provides comprehensive CRUD functionality with proper error handling and validation
 * Implements the generated AuthorsApiDelegate interface for OpenAPI compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorControllerDelegate implements AuthorsApiDelegate {

    private final AuthorRepository authorRepository;

    /**
     * Get all authors with pagination and sorting support
     * Implements AuthorsApiDelegate.getAllAuthors method
     * Uses Spring Boot's automatic Pageable parameter binding for seamless OpenAPI integration
     */
    @Override
    public ResponseEntity<PagedResponse> getAllAuthors(Pageable pageable) {

        log.debug("Fetching authors - pageable: {}", pageable);

        Page<Author> authors = authorRepository.findAll(pageable);

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

        Optional<Author> authorOpt = authorRepository.findById(authorId);

        if (authorOpt.isPresent()) {
            AuthorResponse response = convertToResponse(authorOpt.get());
            log.info("Successfully retrieved author: {}", authorOpt.get().getName());
            return ResponseEntity.ok(response);
        } else {
            throw new NotFoundException("Author not found with ID: " + authorId);
        }
    }

    /**
     * Create a new author
     * Implements AuthorsApiDelegate.createAuthor method
     */
    @Override
    public ResponseEntity<AuthorResponse> createAuthor(AuthorRequest authorRequest) {
        log.debug("Creating new author: {}", authorRequest.getName());

        // Check if author with same name already exists
        if (authorRepository.existsByNameIgnoreCase(authorRequest.getName())) {
            throw new ConflictException("Author already exists with name: " + authorRequest.getName());
        }

        Author author = Author.builder()
                .name(authorRequest.getName())
                .build();

        Author savedAuthor = authorRepository.save(author);
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

        Optional<Author> authorOpt = authorRepository.findById(authorId);

        if (authorOpt.isPresent()) {
            Author author = authorOpt.get();

            // Check if another author with same name exists
            Optional<Author> existingAuthor = authorRepository.findByNameIgnoreCase(authorRequest.getName());
            if (existingAuthor.isPresent() && !existingAuthor.get().getId().equals(authorId)) {
                throw new ConflictException("Author already exists with name: " + authorRequest.getName());
            }

            author.setName(authorRequest.getName());
            Author updatedAuthor = authorRepository.save(author);
            AuthorResponse response = convertToResponse(updatedAuthor);

            log.info("Successfully updated author: {} with ID: {}", updatedAuthor.getName(), updatedAuthor.getId());
            return ResponseEntity.ok(response);

        } else {
            throw new NotFoundException("Author not found with ID: " + authorId);
        }
    }

    /**
     * Delete an author by ID
     * Implements AuthorsApiDelegate.deleteAuthor method
     */
    @Override
    public ResponseEntity<Void> deleteAuthor(Long authorId) {
        log.debug("Deleting author with ID: {}", authorId);

        Optional<Author> authorOpt = authorRepository.findById(authorId);

        if (authorOpt.isPresent()) {
            Author author = authorOpt.get();

            // Check if author has associated books
            if (!author.getBooks().isEmpty()) {
                throw new ConflictException("Cannot delete author " + author.getName() + " - has associated books");
            }

            authorRepository.deleteById(authorId);
            log.info("Successfully deleted author: {} with ID: {}", author.getName(), authorId);
            return ResponseEntity.noContent().build();

        } else {
            throw new NotFoundException("Author not found with ID: " + authorId);
        }
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