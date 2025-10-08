package com.verglor.azul.bookstore.controller;

import com.verglor.azul.bookstore.domain.Author;
import com.verglor.azul.bookstore.domain.Book;
import com.verglor.azul.bookstore.domain.Genre;
import com.verglor.azul.bookstore.exception.BadRequestException;
import com.verglor.azul.bookstore.exception.NotFoundException;
import com.verglor.azul.bookstore.openapi.api.BooksApiDelegate;
import com.verglor.azul.bookstore.openapi.model.*;
import com.verglor.azul.bookstore.openapi.model.PageInfo;
import com.verglor.azul.bookstore.repository.AuthorRepository;
import com.verglor.azul.bookstore.repository.BookRepository;
import com.verglor.azul.bookstore.repository.GenreRepository;
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

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;

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

        Page<Book> books;

        if (title == null & author == null & genre == null) {
            log.debug("Fetching all books");
            books = bookRepository.findAll(pageable);
        } else  {
            log.debug("Searching books by title, author, and genre");
            books = bookRepository.findBooksWithFilters(title, author, genre, pageable);
        }

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

        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (bookOpt.isPresent()) {
            BookResponse response = convertToResponse(bookOpt.get());
            log.info("Successfully retrieved book: {}", bookOpt.get().getTitle());
            return ResponseEntity.ok(response);
        } else {
            throw new NotFoundException("Book not found with ID: " + bookId);
        }
    }

    /**
     * Create a new book
     * Implements BooksApiDelegate.createBook method
     */
    @Override
    public ResponseEntity<BookResponse> createBook(BookRequest bookRequest) {
        log.debug("Creating new book: {}", bookRequest.getTitle());

        // Validate that at least one author is provided
        if (bookRequest.getAuthorIds() == null || bookRequest.getAuthorIds().isEmpty()) {
            throw new BadRequestException("Cannot create book without authors");
        }

        // Validate authors exist
        List<Author> authors = new ArrayList<>();
        for (Long authorId : bookRequest.getAuthorIds()) {
            Optional<Author> authorOpt = authorRepository.findById(authorId);
            if (authorOpt.isEmpty()) {
                throw new BadRequestException("Author not found with ID: " + authorId);
            }
            authors.add(authorOpt.get());
        }

        // Validate genres exist if provided
        List<Genre> genres = new ArrayList<>();
        if (bookRequest.getGenreIds() != null) {
            for (Long genreId : bookRequest.getGenreIds()) {
                Optional<Genre> genreOpt = genreRepository.findById(genreId);
                if (genreOpt.isEmpty()) {
                    throw new BadRequestException("Genre not found with ID: " + genreId);
                }
                genres.add(genreOpt.get());
            }
        }

        Book book = Book.builder()
                .title(bookRequest.getTitle())
                .price(bookRequest.getPrice())
                .authors(authors)
                .genres(genres)
                .build();

        Book savedBook = bookRepository.save(book);
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

        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();

            // Validate that at least one author is provided
            if (bookRequest.getAuthorIds() == null || bookRequest.getAuthorIds().isEmpty()) {
                throw new BadRequestException("Cannot update book without authors");
            }

            // Validate authors exist
            List<Author> authors = new ArrayList<>();
            for (Long authorId : bookRequest.getAuthorIds()) {
                Optional<Author> authorOpt = authorRepository.findById(authorId);
                if (authorOpt.isEmpty()) {
                    throw new BadRequestException("Author not found with ID: " + authorId);
                }
                authors.add(authorOpt.get());
            }

            // Validate genres exist if provided
            List<Genre> genres = new ArrayList<>();
            if (bookRequest.getGenreIds() != null) {
                for (Long genreId : bookRequest.getGenreIds()) {
                    Optional<Genre> genreOpt = genreRepository.findById(genreId);
                    if (genreOpt.isEmpty()) {
                        throw new BadRequestException("Genre not found with ID: " + genreId);
                    }
                    genres.add(genreOpt.get());
                }
            }

            book.setTitle(bookRequest.getTitle());
            book.setPrice(bookRequest.getPrice());
            book.setAuthors(authors);
            book.setGenres(genres);

            Book updatedBook = bookRepository.save(book);
            BookResponse response = convertToResponse(updatedBook);

            log.info("Successfully updated book: {} with ID: {}", updatedBook.getTitle(), updatedBook.getId());
            return ResponseEntity.ok(response);

        } else {
            throw new NotFoundException("Book not found with ID: " + bookId);
        }
    }

    /**
     * Delete a book by ID
     * Implements BooksApiDelegate.deleteBook method
     */
    @Override
    public ResponseEntity<Void> deleteBook(Long bookId) {
        log.debug("Deleting book with ID: {}", bookId);

        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (bookOpt.isPresent()) {
            bookRepository.deleteById(bookId);
            log.info("Successfully deleted book with ID: {}", bookId);
            return ResponseEntity.noContent().build();

        } else {
            throw new NotFoundException("Book not found with ID: " + bookId);
        }
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