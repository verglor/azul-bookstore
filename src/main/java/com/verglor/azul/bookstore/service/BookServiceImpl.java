package com.verglor.azul.bookstore.service;

import com.verglor.azul.bookstore.domain.Author;
import com.verglor.azul.bookstore.domain.Book;
import com.verglor.azul.bookstore.domain.Genre;
import com.verglor.azul.bookstore.exception.BadRequestException;
import com.verglor.azul.bookstore.exception.NotFoundException;
import com.verglor.azul.bookstore.repository.AuthorRepository;
import com.verglor.azul.bookstore.repository.BookRepository;
import com.verglor.azul.bookstore.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for Book entity operations
 * Encapsulates business logic for book management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Book> getAllBooks(String title, String author, String genre, Pageable pageable) {
        log.debug("Fetching books - pageable: {}, title: {}, author: {}, genre: {}", pageable, title, author, genre);

        Page<Book> books;

        if (title == null && author == null && genre == null) {
            log.debug("Fetching all books");
            books = bookRepository.findAll(pageable);
        } else {
            log.debug("Searching books by title, author, and genre");
            books = bookRepository.findBooksWithFilters(title, author, genre, pageable);
        }

        log.info("Successfully retrieved {} books", books.getNumberOfElements());
        return books;
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBookById(Long bookId) {
        log.debug("Fetching book with ID: {}", bookId);

        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (bookOpt.isPresent()) {
            log.info("Successfully retrieved book: {}", bookOpt.get().getTitle());
            return bookOpt.get();
        } else {
            throw new NotFoundException("Book not found with ID: " + bookId);
        }
    }

    @Override
    public Book createBook(String title, BigDecimal price, List<Long> authorIds, List<Long> genreIds) {
        log.debug("Creating new book: {}", title);

        // Validate that at least one author is provided
        if (authorIds == null || authorIds.isEmpty()) {
            throw new BadRequestException("Cannot create book without authors");
        }

        // Validate authors exist
        List<Author> authors = new ArrayList<>();
        for (Long authorId : authorIds) {
            Optional<Author> authorOpt = authorRepository.findById(authorId);
            if (authorOpt.isEmpty()) {
                throw new BadRequestException("Author not found with ID: " + authorId);
            }
            authors.add(authorOpt.get());
        }

        // Validate genres exist if provided
        List<Genre> genres = new ArrayList<>();
        if (genreIds != null) {
            for (Long genreId : genreIds) {
                Optional<Genre> genreOpt = genreRepository.findById(genreId);
                if (genreOpt.isEmpty()) {
                    throw new BadRequestException("Genre not found with ID: " + genreId);
                }
                genres.add(genreOpt.get());
            }
        }

        Book book = Book.builder()
                .title(title)
                .price(price)
                .authors(authors)
                .genres(genres)
                .build();

        Book savedBook = bookRepository.save(book);
        log.info("Successfully created book: {} with ID: {}", savedBook.getTitle(), savedBook.getId());
        return savedBook;
    }

    @Override
    public Book updateBook(Long bookId, String title, BigDecimal price, List<Long> authorIds, List<Long> genreIds) {
        log.debug("Updating book with ID: {} - new title: {}", bookId, title);

        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();

            // Validate that at least one author is provided
            if (authorIds == null || authorIds.isEmpty()) {
                throw new BadRequestException("Cannot update book without authors");
            }

            // Validate authors exist
            List<Author> authors = new ArrayList<>();
            for (Long authorId : authorIds) {
                Optional<Author> authorOpt = authorRepository.findById(authorId);
                if (authorOpt.isEmpty()) {
                    throw new BadRequestException("Author not found with ID: " + authorId);
                }
                authors.add(authorOpt.get());
            }

            // Validate genres exist if provided
            List<Genre> genres = new ArrayList<>();
            if (genreIds != null) {
                for (Long genreId : genreIds) {
                    Optional<Genre> genreOpt = genreRepository.findById(genreId);
                    if (genreOpt.isEmpty()) {
                        throw new BadRequestException("Genre not found with ID: " + genreId);
                    }
                    genres.add(genreOpt.get());
                }
            }

            book.setTitle(title);
            book.setPrice(price);
            book.setAuthors(authors);
            book.setGenres(genres);

            Book updatedBook = bookRepository.save(book);
            log.info("Successfully updated book: {} with ID: {}", updatedBook.getTitle(), updatedBook.getId());
            return updatedBook;

        } else {
            throw new NotFoundException("Book not found with ID: " + bookId);
        }
    }

    @Override
    public void deleteBook(Long bookId) {
        log.debug("Deleting book with ID: {}", bookId);

        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (bookOpt.isPresent()) {
            bookRepository.deleteById(bookId);
            log.info("Successfully deleted book with ID: {}", bookId);
        } else {
            throw new NotFoundException("Book not found with ID: " + bookId);
        }
    }
}