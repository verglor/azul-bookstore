package com.verglor.azul.bookstore.repository;

import com.verglor.azul.bookstore.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepositoryCustom {

    Page<Book> findBooksWithFilters(String title, String authorName, String genreName, Pageable pageable);
}