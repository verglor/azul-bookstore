package com.verglor.azul.bookstore.repository;

import com.verglor.azul.bookstore.domain.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    /**
     * Check if author exists by name (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find author by name (case-insensitive) - returns Optional for null safety
     */
    Optional<Author> findByNameIgnoreCase(String name);

    /**
     * Find authors by name containing (case-insensitive) with pagination
     */
    Page<Author> findByNameIgnoreCaseContaining(String name, Pageable pageable);

}