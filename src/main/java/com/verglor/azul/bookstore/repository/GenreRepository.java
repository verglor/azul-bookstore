package com.verglor.azul.bookstore.repository;

import com.verglor.azul.bookstore.domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    /**
     * Check if genre exists by name (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find genre by name (case-insensitive) - returns Optional for null safety
     */
    Optional<Genre> findByNameIgnoreCase(String name);

}