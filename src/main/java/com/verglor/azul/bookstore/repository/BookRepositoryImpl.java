package com.verglor.azul.bookstore.repository;

import com.verglor.azul.bookstore.domain.Author;
import com.verglor.azul.bookstore.domain.Book;
import com.verglor.azul.bookstore.domain.Genre;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

    private final ObjectProvider<BookRepository> bookRepositoryProvider;

    @Override
    public Page<Book> findBooksWithFilters(String title, String authorName, String genreName, Pageable pageable) {

        Specification<Book> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Title filter
            if (StringUtils.hasText(title)) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }

            // Author name filter
            if (StringUtils.hasText(authorName)) {
                Join<Book, Author> authors = root.join("authors", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(authors.get("name")), "%" + authorName.toLowerCase() + "%"));
            }

            // Genre name filter
            if (StringUtils.hasText(genreName)) {
                Join<Book, Genre> genres = root.join("genres", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(genres.get("name")), "%" + genreName.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));

        };

        return bookRepositoryProvider.getObject().findAll(specification, pageable);

    }
}