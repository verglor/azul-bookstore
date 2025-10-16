package com.verglor.azul.bookstore.service;

import com.verglor.azul.bookstore.domain.Author;
import com.verglor.azul.bookstore.exception.ConflictException;
import com.verglor.azul.bookstore.exception.NotFoundException;
import com.verglor.azul.bookstore.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service implementation for Author entity operations
 * Encapsulates business logic for author management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Author> getAllAuthors(String name, Pageable pageable) {
        log.debug("Fetching authors - name: {}, pageable: {}", name, pageable);

        Page<Author> authors;
        if (name != null && !name.trim().isEmpty()) {
            authors = authorRepository.findByNameIgnoreCaseContaining(name.trim(), pageable);
        } else {
            authors = authorRepository.findAll(pageable);
        }

        log.info("Successfully retrieved {} authors", authors.getNumberOfElements());
        return authors;
    }

    @Override
    @Transactional(readOnly = true)
    public Author getAuthorById(Long authorId) {
        log.debug("Fetching author with ID: {}", authorId);

        Optional<Author> authorOpt = authorRepository.findById(authorId);

        if (authorOpt.isPresent()) {
            log.info("Successfully retrieved author: {}", authorOpt.get().getName());
            return authorOpt.get();
        } else {
            throw new NotFoundException("Author not found with ID: " + authorId);
        }
    }

    @Override
    public Author createAuthor(String name) {
        log.debug("Creating new author: {}", name);

        // Check if author with same name already exists
        if (authorRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Author already exists with name: " + name);
        }

        Author author = Author.builder()
                .name(name)
                .build();

        Author savedAuthor = authorRepository.save(author);
        log.info("Successfully created author: {} with ID: {}", savedAuthor.getName(), savedAuthor.getId());
        return savedAuthor;
    }

    @Override
    public Author updateAuthor(Long authorId, String name) {
        log.debug("Updating author with ID: {} - new name: {}", authorId, name);

        Optional<Author> authorOpt = authorRepository.findById(authorId);

        if (authorOpt.isPresent()) {
            Author author = authorOpt.get();

            // Check if another author with same name exists
            Optional<Author> existingAuthor = authorRepository.findByNameIgnoreCase(name);
            if (existingAuthor.isPresent() && !existingAuthor.get().getId().equals(authorId)) {
                throw new ConflictException("Author already exists with name: " + name);
            }

            author.setName(name);
            Author updatedAuthor = authorRepository.save(author);
            log.info("Successfully updated author: {} with ID: {}", updatedAuthor.getName(), updatedAuthor.getId());
            return updatedAuthor;

        } else {
            throw new NotFoundException("Author not found with ID: " + authorId);
        }
    }

    @Override
    public void deleteAuthor(Long authorId) {
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

        } else {
            throw new NotFoundException("Author not found with ID: " + authorId);
        }
    }
}