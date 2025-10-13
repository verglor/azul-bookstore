package com.verglor.azul.bookstore

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import com.verglor.azul.bookstore.repository.AuthorRepository
import com.verglor.azul.bookstore.repository.BookRepository
import com.verglor.azul.bookstore.repository.GenreRepository
import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import static io.restassured.RestAssured.given

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseIntegrationSpec extends Specification {

    @LocalServerPort
    int port

    @Autowired
    BookRepository bookRepository

    @Autowired
    AuthorRepository authorRepository

    @Autowired
    GenreRepository genreRepository

    def setup() {
        RestAssured.port = port

        // Clean up database before each test
        bookRepository.deleteAll()
        authorRepository.deleteAll()
        genreRepository.deleteAll()
    }

    def cleanup() {
        // Clean up after each test
        bookRepository.deleteAll()
        authorRepository.deleteAll()
        genreRepository.deleteAll()
    }

    protected RequestSpecification getRequestSpecification() {
        return given()
                .contentType("application/json")
                .accept("application/json")
    }

    protected String getBaseUrl() {
        return "http://localhost:${port}/api/v1"
    }

    // Helper methods for creating test data
    protected Book createTestBook(String title, BigDecimal price) {
        return Book.builder()
                .title(title)
                .price(price)
                .authors(new ArrayList<>())
                .genres(new ArrayList<>())
                .build()
    }

    protected Author createTestAuthor(String name) {
        return Author.builder()
                .name(name)
                .books(new ArrayList<>())
                .build()
    }

    protected Genre createTestGenre(String name) {
        return Genre.builder()
                .name(name)
                .books(new ArrayList<>())
                .build()
    }

    protected Map<String, Object> createBookRequest(String title, BigDecimal price, List<Long> authorIds = null, List<Long> genreIds = null) {
        Map<String, Object> request = [
                title: title,
                price: price
        ]

        if (authorIds) {
            request.authorIds = authorIds
        }

        if (genreIds) {
            request.genreIds = genreIds
        }

        return request
    }

    protected Map<String, Object> createAuthorRequest(String name) {
        return [
                name: name
        ]
    }

    protected Map<String, Object> createGenreRequest(String name) {
        return [
                name: name
        ]
    }
}