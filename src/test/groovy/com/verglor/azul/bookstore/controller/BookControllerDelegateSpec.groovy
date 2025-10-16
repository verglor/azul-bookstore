package com.verglor.azul.bookstore.controller

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import com.verglor.azul.bookstore.exception.BadRequestException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.openapi.model.*
import com.verglor.azul.bookstore.service.BookService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class BookControllerDelegateSpec extends Specification {

    BookService bookService = Mock()

    @Subject
    BookControllerDelegate bookControllerDelegate = new BookControllerDelegate(bookService)

    def "should get all books without filters"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Book> books = [createTestBook(1L, "Book 1", 19.99), createTestBook(2L, "Book 2", 24.99)]
        Page<Book> bookPage = new PageImpl<>(books, pageable, 2)

        when:
        bookService.getAllBooks(null, null, null, pageable) >> bookPage
        ResponseEntity<PagedResponse> response = bookControllerDelegate.getAllBooks(null, null, null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 2
        response.body.content[0].title == "Book 1"
        response.body.content[1].title == "Book 2"
        response.body.page.totalElements == 2
    }

    def "should get book by id successfully"() {
        given:
        Long bookId = 1L
        Book book = createTestBook(bookId, "Test Book", 19.99)

        when:
        bookService.getBookById(bookId) >> book
        ResponseEntity<BookResponse> response = bookControllerDelegate.getBookById(bookId)

        then:
        response.statusCode == HttpStatus.OK
        response.body.id == bookId
        response.body.title == "Test Book"
        response.body.price == 19.99
    }

    def "should return not found when book doesn't exist"() {
        given:
        Long bookId = 999L

        when:
        bookService.getBookById(bookId) >> { throw new NotFoundException("Book not found") }
        bookControllerDelegate.getBookById(bookId)

        then:
        thrown(NotFoundException)
    }

    def "should create book successfully"() {
        given:
        BookRequest bookRequest = BookRequest.builder()
                .title("New Book")
                .price(29.99)
                .authorIds([1L, 2L])
                .genreIds([1L])
                .build()

        Book savedBook = createTestBook(1L, "New Book", 29.99)

        when:
        bookService.createBook("New Book", 29.99, [1L, 2L], [1L]) >> savedBook
        ResponseEntity<BookResponse> response = bookControllerDelegate.createBook(bookRequest)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.title == "New Book"
        response.body.price == 29.99
    }

    def "should return bad request when creating book without authors"() {
        given:
        BookRequest bookRequest = BookRequest.builder()
                .title("Book Without Authors")
                .price(19.99)
                .authorIds([])
                .build()

        when:
        bookService.createBook("Book Without Authors", 19.99, [], []) >> { throw new BadRequestException("Book must have at least one author") }
        bookControllerDelegate.createBook(bookRequest)

        then:
        thrown(BadRequestException)
    }

    def "should return bad request when creating book with non-existent author"() {
        given:
        BookRequest bookRequest = BookRequest.builder()
                .title("Book With Invalid Author")
                .price(19.99)
                .authorIds([999L])
                .build()

        when:
        bookService.createBook("Book With Invalid Author", 19.99, [999L], []) >> { throw new BadRequestException("Author not found") }
        bookControllerDelegate.createBook(bookRequest)

        then:
        thrown(BadRequestException)
    }

    def "should return bad request when creating book with non-existent genre"() {
        given:
        BookRequest bookRequest = BookRequest.builder()
                .title("Book With Invalid Genre")
                .price(19.99)
                .authorIds([1L])
                .genreIds([999L])
                .build()

        when:
        bookService.createBook("Book With Invalid Genre", 19.99, [1L], [999L]) >> { throw new BadRequestException("Genre not found") }
        bookControllerDelegate.createBook(bookRequest)

        then:
        thrown(BadRequestException)
    }

    def "should update book successfully"() {
        given:
        Long bookId = 1L
        BookRequest bookRequest = BookRequest.builder()
                .title("Updated Book")
                .price(39.99)
                .authorIds([1L])
                .genreIds([1L])
                .build()

        Book updatedBook = createTestBook(bookId, "Updated Book", 39.99)

        when:
        bookService.updateBook(bookId, "Updated Book", 39.99, [1L], [1L]) >> updatedBook
        ResponseEntity<BookResponse> response = bookControllerDelegate.updateBook(bookId, bookRequest)

        then:
        response.statusCode == HttpStatus.OK
        response.body.title == "Updated Book"
        response.body.price == 39.99
    }

    def "should return not found when updating non-existent book"() {
        given:
        Long bookId = 999L
        BookRequest bookRequest = BookRequest.builder()
                .title("Updated Book")
                .price(39.99)
                .authorIds([1L])
                .build()

        when:
        bookService.updateBook(bookId, "Updated Book", 39.99, [1L], []) >> { throw new NotFoundException("Book not found") }
        bookControllerDelegate.updateBook(bookId, bookRequest)

        then:
        thrown(NotFoundException)
    }

    def "should return bad request when updating book without authors"() {
        given:
        Long bookId = 1L
        BookRequest bookRequest = BookRequest.builder()
                .title("Updated Book")
                .price(39.99)
                .authorIds([])
                .build()

        when:
        bookService.updateBook(bookId, "Updated Book", 39.99, [], []) >> { throw new BadRequestException("Book must have at least one author") }
        bookControllerDelegate.updateBook(bookId, bookRequest)

        then:
        thrown(BadRequestException)
    }

    def "should delete book successfully"() {
        given:
        Long bookId = 1L

        when:
        bookService.deleteBook(bookId) >> {}
        ResponseEntity<Void> response = bookControllerDelegate.deleteBook(bookId)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
        response.body == null
    }

    def "should return not found when deleting non-existent book"() {
        given:
        Long bookId = 999L

        when:
        bookService.deleteBook(bookId) >> { throw new NotFoundException("Book not found") }
        bookControllerDelegate.deleteBook(bookId)

        then:
        thrown(NotFoundException)
    }


    // Helper methods
    private Book createTestBook(Long id, String title, BigDecimal price) {
        return Book.builder()
                .id(id)
                .title(title)
                .price(price)
                .authors([])
                .genres([])
                .build()
    }

    private Author createTestAuthor(Long id, String name) {
        return Author.builder()
                .id(id)
                .name(name)
                .books([])
                .build()
    }

    private Genre createTestGenre(Long id, String name) {
        return Genre.builder()
                .id(id)
                .name(name)
                .books([])
                .build()
    }
}