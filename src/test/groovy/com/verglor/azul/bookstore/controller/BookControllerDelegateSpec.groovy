package com.verglor.azul.bookstore.controller

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import com.verglor.azul.bookstore.exception.BadRequestException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.openapi.model.*
import com.verglor.azul.bookstore.repository.AuthorRepository
import com.verglor.azul.bookstore.repository.BookRepository
import com.verglor.azul.bookstore.repository.GenreRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class BookControllerDelegateSpec extends Specification {

    BookRepository bookRepository = Mock()
    AuthorRepository authorRepository = Mock()
    GenreRepository genreRepository = Mock()

    @Subject
    BookControllerDelegate bookControllerDelegate = new BookControllerDelegate(bookRepository, authorRepository, genreRepository)

    def "should get all books without filters"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Book> books = [createTestBook(1L, "Book 1", 19.99), createTestBook(2L, "Book 2", 24.99)]
        Page<Book> bookPage = new PageImpl<>(books, pageable, 2)

        when:
        bookRepository.findAll(pageable) >> bookPage
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
        bookRepository.findById(bookId) >> Optional.of(book)
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
        bookRepository.findById(bookId) >> Optional.empty()
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

        Author author1 = createTestAuthor(1L, "Author 1")
        Author author2 = createTestAuthor(2L, "Author 2")
        Genre genre1 = createTestGenre(1L, "Fiction")
        Book savedBook = createTestBook(1L, "New Book", 29.99)

        when:
        authorRepository.findById(1L) >> Optional.of(author1)
        authorRepository.findById(2L) >> Optional.of(author2)
        genreRepository.findById(1L) >> Optional.of(genre1)
        bookRepository.save(_) >> savedBook
        ResponseEntity<BookResponse> response = bookControllerDelegate.createBook(bookRequest)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.title == "New Book"
        response.body.price == 29.99
        1 * bookRepository.save(_) >> { Book book ->
            assert book.title == "New Book"
            assert book.price == 29.99
            assert book.authors.size() == 2
            assert book.genres.size() == 1
            return savedBook
        }
    }

    def "should return bad request when creating book without authors"() {
        given:
        BookRequest bookRequest = BookRequest.builder()
                .title("Book Without Authors")
                .price(19.99)
                .authorIds([])
                .build()

        when:
        bookControllerDelegate.createBook(bookRequest)

        then:
        thrown(BadRequestException)
        0 * bookRepository.save(_)
    }

    def "should return bad request when creating book with non-existent author"() {
        given:
        BookRequest bookRequest = BookRequest.builder()
                .title("Book With Invalid Author")
                .price(19.99)
                .authorIds([999L])
                .build()

        when:
        authorRepository.findById(999L) >> Optional.empty()
        bookControllerDelegate.createBook(bookRequest)

        then:
        thrown(BadRequestException)
        0 * bookRepository.save(_)
    }

    def "should return bad request when creating book with non-existent genre"() {
        given:
        BookRequest bookRequest = BookRequest.builder()
                .title("Book With Invalid Genre")
                .price(19.99)
                .authorIds([1L])
                .genreIds([999L])
                .build()

        Author author1 = createTestAuthor(1L, "Author 1")

        when:
        authorRepository.findById(1L) >> Optional.of(author1)
        genreRepository.findById(999L) >> Optional.empty()
        bookControllerDelegate.createBook(bookRequest)

        then:
        thrown(BadRequestException)
        0 * bookRepository.save(_)
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

        Book existingBook = createTestBook(bookId, "Original Book", 29.99)
        Author author1 = createTestAuthor(1L, "Author 1")
        Genre genre1 = createTestGenre(1L, "Fiction")
        Book updatedBook = createTestBook(bookId, "Updated Book", 39.99)

        when:
        bookRepository.findById(bookId) >> Optional.of(existingBook)
        authorRepository.findById(1L) >> Optional.of(author1)
        genreRepository.findById(1L) >> Optional.of(genre1)
        bookRepository.save(_) >> updatedBook
        ResponseEntity<BookResponse> response = bookControllerDelegate.updateBook(bookId, bookRequest)

        then:
        response.statusCode == HttpStatus.OK
        response.body.title == "Updated Book"
        response.body.price == 39.99
        1 * bookRepository.save(_) >> { Book book ->
            assert book.title == "Updated Book"
            assert book.price == 39.99
            return updatedBook
        }
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
        bookRepository.findById(bookId) >> Optional.empty()
        bookControllerDelegate.updateBook(bookId, bookRequest)

        then:
        thrown(NotFoundException)
        0 * bookRepository.save(_)
    }

    def "should return bad request when updating book without authors"() {
        given:
        Long bookId = 1L
        BookRequest bookRequest = BookRequest.builder()
                .title("Updated Book")
                .price(39.99)
                .authorIds([])
                .build()

        Book existingBook = createTestBook(bookId, "Original Book", 29.99)

        when:
        bookRepository.findById(bookId) >> Optional.of(existingBook)
        bookControllerDelegate.updateBook(bookId, bookRequest)

        then:
        thrown(BadRequestException)
        0 * bookRepository.save(_)
    }

    def "should delete book successfully"() {
        given:
        Long bookId = 1L
        Book existingBook = createTestBook(bookId, "Book to Delete", 19.99)

        when:
        bookRepository.findById(bookId) >> Optional.of(existingBook)
        ResponseEntity<Void> response = bookControllerDelegate.deleteBook(bookId)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
        response.body == null
        1 * bookRepository.deleteById(bookId)
    }

    def "should return not found when deleting non-existent book"() {
        given:
        Long bookId = 999L

        when:
        bookRepository.findById(bookId) >> Optional.empty()
        bookControllerDelegate.deleteBook(bookId)

        then:
        thrown(NotFoundException)
        0 * bookRepository.deleteById(_)
    }

    def "should handle pagination metadata correctly"() {
        given:
        Pageable pageable = PageRequest.of(1, 5) // Second page, 5 items per page
        List<Book> books = [createTestBook(1L, "Book 1", 19.99)]
        Page<Book> bookPage = new PageImpl<>(books, pageable, 12) // Total 12 items

        when:
        bookRepository.findAll(pageable) >> bookPage
        ResponseEntity<PagedResponse> response = bookControllerDelegate.getAllBooks(null, null, null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.page.size == 5
        response.body.page.number == 1
        response.body.page.totalElements == 12
        response.body.page.totalPages == 3 // 12 items / 5 per page = 3 pages
    }

    def "should convert book with authors and genres correctly"() {
        given:
        Long bookId = 1L
        Book book = createTestBook(bookId, "Test Book", 29.99)
        book.authors = [createTestAuthor(1L, "Author 1"), createTestAuthor(2L, "Author 2")]
        book.genres = [createTestGenre(1L, "Fiction"), createTestGenre(2L, "Adventure")]

        when:
        bookRepository.findById(bookId) >> Optional.of(book)
        ResponseEntity<BookResponse> response = bookControllerDelegate.getBookById(bookId)

        then:
        response.statusCode == HttpStatus.OK
        response.body.authors.size() == 2
        response.body.authors*.name.containsAll(["Author 1", "Author 2"])
        response.body.genres.size() == 2
        response.body.genres*.name.containsAll(["Fiction", "Adventure"])
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