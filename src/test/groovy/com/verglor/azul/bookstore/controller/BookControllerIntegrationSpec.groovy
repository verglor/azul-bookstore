package com.verglor.azul.bookstore.controller

import com.verglor.azul.bookstore.BaseIntegrationSpec
import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import org.springframework.transaction.annotation.Transactional

import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.*

class BookControllerIntegrationSpec extends BaseIntegrationSpec {

    def "should get all books successfully"() {
        given:
        // Create test data
        Author author = createTestAuthor("Test Author")
        Genre genre = createTestGenre("Fiction")
        authorRepository.save(author)
        genreRepository.save(genre)

        Book book1 = createTestBook("Book One", 19.99)
        Book book2 = createTestBook("Book Two", 24.99)
        book1.authors.add(author)
        book1.genres.add(genre)
        book2.authors.add(author)
        book2.genres.add(genre)
        bookRepository.save(book1)
        bookRepository.save(book2)

        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].title", notNullValue())
                .body("content[0].price", notNullValue())
                .body("content[0].authors", notNullValue())
                .body("content[0].genres", notNullValue())
                .body("page.totalElements", equalTo(2))
                .body("page.totalPages", equalTo(1))
                .body("page.size", equalTo(20))
                .body("page.number", equalTo(0))
    }

    def "should get book by id successfully"() {
        given:
        Author author = createTestAuthor("Test Author")
        Genre genre = createTestGenre("Fiction")
        authorRepository.save(author)
        genreRepository.save(genre)

        Book book = createTestBook("Test Book", 29.99)
        book.authors.add(author)
        book.genres.add(genre)
        Book savedBook = bookRepository.save(book)

        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/books/${savedBook.id}")

        then:
        response.then()
                .statusCode(200)
                .body("id", equalTo(savedBook.id.intValue()))
                .body("title", equalTo("Test Book"))
                .body("price", equalTo(29.99f))
                .body("authors.size()", equalTo(1))
                .body("authors[0].name", equalTo("Test Author"))
                .body("genres.size()", equalTo(1))
                .body("genres[0].name", equalTo("Fiction"))
    }

    def "should return 404 when book not found"() {
        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/books/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Book not found with ID: 999"))
    }

    def "should create book successfully"() {
        given:
        Author author = createTestAuthor("Test Author")
        Genre genre = createTestGenre("Fiction")
        authorRepository.save(author)
        genreRepository.save(genre)

        Map<String, Object> bookRequest = createBookRequest("New Book", 39.99, [author.id], [genre.id])

        when:
        def response = getRequestSpecification()
                .body(bookRequest)
                .when()
                .post("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("New Book"))
                .body("price", equalTo(39.99f))
                .body("authors.size()", equalTo(1))
                .body("authors[0].name", equalTo("Test Author"))
                .body("genres.size()", equalTo(1))
                .body("genres[0].name", equalTo("Fiction"))
    }

    def "should return 400 when creating book with invalid data"() {
        given:
        Map<String, Object> bookRequest = [
                title: "", // Invalid empty title
                price: -10.00 // Invalid negative price
        ]

        when:
        def response = getRequestSpecification()
                .body(bookRequest)
                .when()
                .post("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors", notNullValue())
    }

    def "should return 400 when creating book with non-existent author"() {
        given:
        Map<String, Object> bookRequest = createBookRequest("Book with Invalid Author", 19.99, [999L])

        when:
        def response = getRequestSpecification()
                .body(bookRequest)
                .when()
                .post("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
    }

    def "should return 400 when creating book with non-existent genre"() {
        given:
        Author author = createTestAuthor("Test Author")
        authorRepository.save(author)

        Map<String, Object> bookRequest = createBookRequest("Book with Invalid Genre", 19.99, [author.id], [999L])

        when:
        def response = getRequestSpecification()
                .body(bookRequest)
                .when()
                .post("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
    }

    def "should update book successfully"() {
        given:
        Author author1 = createTestAuthor("Original Author")
        Author author2 = createTestAuthor("New Author")
        Genre genre1 = createTestGenre("Original Genre")
        Genre genre2 = createTestGenre("New Genre")
        authorRepository.saveAll([author1, author2])
        genreRepository.saveAll([genre1, genre2])

        Book book = createTestBook("Original Book", 19.99)
        book.authors.add(author1)
        book.genres.add(genre1)
        Book savedBook = bookRepository.save(book)

        Map<String, Object> updateRequest = createBookRequest("Updated Book", 29.99, [author2.id], [genre2.id])

        when:
        def response = getRequestSpecification()
                .body(updateRequest)
                .when()
                .put("${getBaseUrl()}/books/${savedBook.id}")

        then:
        response.then()
                .statusCode(200)
                .body("id", equalTo(savedBook.id.intValue()))
                .body("title", equalTo("Updated Book"))
                .body("price", equalTo(29.99f))
                .body("authors.size()", equalTo(1))
                .body("authors[0].name", equalTo("New Author"))
                .body("genres.size()", equalTo(1))
                .body("genres[0].name", equalTo("New Genre"))
    }

    def "should return 404 when updating non-existent book"() {
        given:
        Map<String, Object> updateRequest = createBookRequest("Updated Book", 29.99)

        when:
        def response = getRequestSpecification()
                .body(updateRequest)
                .when()
                .put("${getBaseUrl()}/books/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
    }

    def "should delete book successfully"() {
        given:
        Book book = createTestBook("Book to Delete", 19.99)
        Book savedBook = bookRepository.save(book)

        when:
        def response = getRequestSpecification()
                .when()
                .delete("${getBaseUrl()}/books/${savedBook.id}")

        then:
        response.then()
                .statusCode(204)

        and:
        // Verify book is deleted
        given()
                .contentType("application/json")
                .accept("application/json")
                .when()
                .get("${getBaseUrl()}/books/${savedBook.id}")
                .then()
                .statusCode(404)
    }

    def "should return 404 when deleting non-existent book"() {
        when:
        def response = getRequestSpecification()
                .when()
                .delete("${getBaseUrl()}/books/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
    }

    def "should handle pagination correctly"() {
        given:
        // Create 5 books
        (1..5).each { i ->
            Book book = createTestBook("Book $i", 10.00 + i)
            bookRepository.save(book)
        }

        when:
        def response = getRequestSpecification()
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("page.size", equalTo(2))
                .body("page.number", equalTo(0))
                .body("page.totalElements", equalTo(5))
                .body("page.totalPages", equalTo(3))
    }

    def "should handle sorting correctly"() {
        given:
        Book book1 = createTestBook("Z Book", 19.99)
        Book book2 = createTestBook("A Book", 24.99)
        bookRepository.save(book1)
        bookRepository.save(book2)

        when:
        def response = getRequestSpecification()
                .queryParam("sort", "title,asc")
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].title", equalTo("A Book"))
                .body("content[1].title", equalTo("Z Book"))
    }

    def "should handle search by title"() {
        given:
        Book book1 = createTestBook("Java Programming Guide", 39.99)
        Book book2 = createTestBook("Python Basics", 29.99)
        Book book3 = createTestBook("JavaScript Handbook", 34.99)
        bookRepository.saveAll([book1, book2, book3])

        when:
        def response = getRequestSpecification()
                .queryParam("title", "Java")
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content*.title", hasItems("Java Programming Guide", "JavaScript Handbook"))
    }

    def "should handle search by author name"() {
        given:
        Author author1 = createTestAuthor("John Doe")
        Author author2 = createTestAuthor("Jane Smith")
        authorRepository.saveAll([author1, author2])

        Book book1 = createTestBook("Book 1", 19.99)
        Book book2 = createTestBook("Book 2", 24.99)
        book1.authors.add(author1)
        book2.authors.add(author2)
        bookRepository.saveAll([book1, book2])

        when:
        def response = getRequestSpecification()
                .queryParam("author", "John")
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].title", equalTo("Book 1"))
    }

    def "should handle search by genre name"() {
        given:
        Genre genre1 = createTestGenre("Fiction")
        Genre genre2 = createTestGenre("Science")
        genreRepository.saveAll([genre1, genre2])

        Book book1 = createTestBook("Fiction Book", 19.99)
        Book book2 = createTestBook("Science Book", 24.99)
        book1.genres.add(genre1)
        book2.genres.add(genre2)
        bookRepository.saveAll([book1, book2])

        when:
        def response = getRequestSpecification()
                .queryParam("genre", "Fiction")
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].title", equalTo("Fiction Book"))
    }

    def "should handle multiple search filters"() {
        given:
        Author author = createTestAuthor("Test Author")
        Genre genre = createTestGenre("Test Genre")
        authorRepository.save(author)
        genreRepository.save(genre)

        Book matchingBook = createTestBook("Matching Book", 29.99)
        matchingBook.authors.add(author)
        matchingBook.genres.add(genre)

        Book nonMatchingBook = createTestBook("Non-Matching Book", 19.99)
        bookRepository.saveAll([matchingBook, nonMatchingBook])

        when:
        def response = getRequestSpecification()
                .queryParam("title", "Matching")
                .queryParam("author", "Author")
                .queryParam("genre", "Genre")
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].title", equalTo("Matching Book"))
    }

    def "should return empty result when no books match search"() {
        given:
        Book book = createTestBook("Test Book", 19.99)
        bookRepository.save(book)

        when:
        def response = getRequestSpecification()
                .queryParam("title", "NonExistent")
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("page.totalElements", equalTo(0))
    }

    def "should handle case insensitive search"() {
        given:
        Book book = createTestBook("SPRING Boot Guide", 39.99)
        bookRepository.save(book)

        when:
        def response = getRequestSpecification()
                .queryParam("title", "spring")
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].title", equalTo("SPRING Boot Guide"))
    }

    def "should handle sorting by price descending"() {
        given:
        Book book1 = createTestBook("Book 1", 50.00)
        Book book2 = createTestBook("Book 2", 30.00)
        Book book3 = createTestBook("Book 3", 40.00)
        bookRepository.saveAll([book1, book2, book3])

        when:
        def response = getRequestSpecification()
                .queryParam("sort", "price,desc")
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("content[0].price", equalTo(50.00f))
                .body("content[1].price", equalTo(40.00f))
                .body("content[2].price", equalTo(30.00f))
    }

    def "should handle complex sorting with multiple fields"() {
        given:
        Book book1 = createTestBook("Book A", 30.00)
        Book book2 = createTestBook("Book A", 20.00)
        Book book3 = createTestBook("Book B", 30.00)
        bookRepository.saveAll([book1, book2, book3])

        when:
        def response = getRequestSpecification()
                .queryParam("sort", ["title,asc","price,desc"])
                .when()
                .get("${getBaseUrl()}/books")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("content[0].title", equalTo("Book A"))
                .body("content[0].price", equalTo(30.00f))
                .body("content[1].title", equalTo("Book A"))
                .body("content[1].price", equalTo(20.00f))
                .body("content[2].title", equalTo("Book B"))
    }
}