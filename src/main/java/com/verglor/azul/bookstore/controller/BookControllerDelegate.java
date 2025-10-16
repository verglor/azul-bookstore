package com.verglor.azul.bookstore.controller;

import com.verglor.azul.bookstore.domain.Author;
import com.verglor.azul.bookstore.domain.Book;
import com.verglor.azul.bookstore.domain.Genre;
import com.verglor.azul.bookstore.openapi.api.BooksApiDelegate;
import com.verglor.azul.bookstore.openapi.model.*;
import com.verglor.azul.bookstore.openapi.model.PageInfo;
import com.verglor.azul.bookstore.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Book entity operations
 * Provides comprehensive CRUD functionality with proper error handling and validation
 * Implements the generated BooksApiDelegate interface for OpenAPI compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookControllerDelegate implements BooksApiDelegate {

    private final BookService bookService;

    /**
     * Get all books with pagination, sorting, and search support
     * Implements BooksApiDelegate.getAllBooks method
     * Uses Spring Boot's automatic Pageable parameter binding for seamless OpenAPI integration
     * Supports search by title, author, and genre parameters
     */
    @Override
    public ResponseEntity<PagedResponse> getAllBooks(
            String title,
            String author,
            String genre,
            org.springframework.data.domain.Pageable pageable) {

        log.debug("Fetching books - pageable: {}, title: {}, author: {}, genre: {}", pageable, title, author, genre);

        Page<Book> books = bookService.getAllBooks(title, author, genre, pageable);

        PagedResponse response = convertToPagedResponse(books);

        log.info("Successfully retrieved {} books", books.getNumberOfElements());
        return ResponseEntity.ok(response);
    }


    /**
     * Get book by ID
     * Implements BooksApiDelegate.getBookById method
     */
    @Override
    public ResponseEntity<BookResponse> getBookById(Long bookId) {
        log.debug("Fetching book with ID: {}", bookId);

        Book book = bookService.getBookById(bookId);
        BookResponse response = convertToResponse(book);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new book
     * Implements BooksApiDelegate.createBook method
     */
    @Override
    public ResponseEntity<BookResponse> createBook(BookRequest bookRequest) {
        log.debug("Creating new book: {}", bookRequest.getTitle());

        Book savedBook = bookService.createBook(bookRequest.getTitle(), bookRequest.getPrice(),
                bookRequest.getAuthorIds(), bookRequest.getGenreIds());
        BookResponse response = convertToResponse(savedBook);

        log.info("Successfully created book: {} with ID: {}", savedBook.getTitle(), savedBook.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing book
     * Implements BooksApiDelegate.updateBook method
     */
    @Override
    public ResponseEntity<BookResponse> updateBook(Long bookId, BookRequest bookRequest) {

        log.debug("Updating book with ID: {} - new title: {}", bookId, bookRequest.getTitle());

        Book updatedBook = bookService.updateBook(bookId, bookRequest.getTitle(), bookRequest.getPrice(),
                bookRequest.getAuthorIds(), bookRequest.getGenreIds());
        BookResponse response = convertToResponse(updatedBook);

        log.info("Successfully updated book: {} with ID: {}", updatedBook.getTitle(), updatedBook.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a book by ID
     * Implements BooksApiDelegate.deleteBook method
     */
    @Override
    public ResponseEntity<Void> deleteBook(Long bookId) {
        log.debug("Deleting book with ID: {}", bookId);

        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }


    /**
     * Convert Book entity to BookResponse DTO
     */
    private BookResponse convertToResponse(Book book) {
        List<AuthorResponse> authorResponses = book.getAuthors().stream()
                .map(author -> AuthorResponse.builder()
                        .id(author.getId())
                        .name(author.getName())
                        .build())
                .collect(Collectors.toList());

        List<GenreResponse> genreResponses = book.getGenres().stream()
                .map(genre -> GenreResponse.builder()
                        .id(genre.getId())
                        .name(genre.getName())
                        .build())
                .collect(Collectors.toList());

        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .price(book.getPrice())
                .authors(authorResponses)
                .genres(genreResponses)
                .build();
    }


    /**
     * Convert Page<Book> to PagedResponse for interface compliance
     */
    private PagedResponse convertToPagedResponse(Page<Book> books) {
        @SuppressWarnings("unchecked")
        List<PagedResponseContentInner> content = (List<PagedResponseContentInner>) (List<?>) books.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        PageInfo pageInfo = PageInfo.builder()
                .size(books.getSize())
                .totalElements(books.getTotalElements())
                .totalPages(books.getTotalPages())
                .number(books.getNumber())
                .build();

        return PagedResponse.builder()
                .content(content)
                .page(pageInfo)
                .build();
    }
}