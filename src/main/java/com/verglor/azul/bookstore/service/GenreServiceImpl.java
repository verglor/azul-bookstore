package com.verglor.azul.bookstore.service;

import com.verglor.azul.bookstore.domain.Genre;
import com.verglor.azul.bookstore.exception.ConflictException;
import com.verglor.azul.bookstore.exception.NotFoundException;
import com.verglor.azul.bookstore.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service implementation for Genre entity operations
 * Encapsulates business logic for genre management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Genre> getAllGenres(String name, Pageable pageable) {
        log.debug("Fetching genres - name: {}, pageable: {}", name, pageable);

        Page<Genre> genres;
        if (name != null && !name.trim().isEmpty()) {
            genres = genreRepository.findByNameIgnoreCaseContaining(name.trim(), pageable);
        } else {
            genres = genreRepository.findAll(pageable);
        }

        log.info("Successfully retrieved {} genres", genres.getNumberOfElements());
        return genres;
    }

    @Override
    @Transactional(readOnly = true)
    public Genre getGenreById(Long genreId) {
        log.debug("Fetching genre with ID: {}", genreId);

        Optional<Genre> genreOpt = genreRepository.findById(genreId);

        if (genreOpt.isPresent()) {
            log.info("Successfully retrieved genre: {}", genreOpt.get().getName());
            return genreOpt.get();
        } else {
            throw new NotFoundException("Genre not found with ID: " + genreId);
        }
    }

    @Override
    public Genre createGenre(String name) {
        log.debug("Creating new genre: {}", name);

        // Check if genre with same name already exists
        if (genreRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Genre already exists with name: " + name);
        }

        Genre genre = Genre.builder()
                .name(name)
                .build();

        Genre savedGenre = genreRepository.save(genre);
        log.info("Successfully created genre: {} with ID: {}", savedGenre.getName(), savedGenre.getId());
        return savedGenre;
    }

    @Override
    public Genre updateGenre(Long genreId, String name) {
        log.debug("Updating genre with ID: {} - new name: {}", genreId, name);

        Optional<Genre> genreOpt = genreRepository.findById(genreId);

        if (genreOpt.isPresent()) {
            Genre genre = genreOpt.get();

            // Check if another genre with same name exists
            Optional<Genre> existingGenre = genreRepository.findByNameIgnoreCase(name);
            if (existingGenre.isPresent() && !existingGenre.get().getId().equals(genreId)) {
                throw new ConflictException("Another genre already exists with name: " + name);
            }

            genre.setName(name);
            Genre updatedGenre = genreRepository.save(genre);
            log.info("Successfully updated genre: {} with ID: {}", updatedGenre.getName(), updatedGenre.getId());
            return updatedGenre;

        } else {
            throw new NotFoundException("Genre not found with ID: " + genreId);
        }
    }

    @Override
    public void deleteGenre(Long genreId) {
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

        } else {
            throw new NotFoundException("Genre not found with ID: " + genreId);
        }
    }
}