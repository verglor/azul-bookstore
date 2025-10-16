package com.verglor.azul.bookstore.service

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import com.verglor.azul.bookstore.exception.BadRequestException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.repository.AuthorRepository
import com.verglor.azul.bookstore.repository.BookRepository
import com.verglor.azul.bookstore.repository.GenreRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal