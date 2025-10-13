package com.verglor.azul.bookstore.repository

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookRepositoryIntegrationSpec extends Specification {

    @Autowired
    BookRepository bookRepository

    @Autowired
    AuthorRepository authorRepository

    @Autowired
    GenreRepository genreRepository

    def setup() {
        // Clean up database before each test
        bookRepository.deleteAll()
        authorRepository.deleteAll()
        genreRepository.deleteAll()
    }

    def "should save and find book by id"() {
        given:
        Book book = createTestBook("Test Book", 29.99)

        when:
        Book savedBook = bookRepository.save(book)

        then:
        savedBook.id != null
        savedBook.title == "Test Book"
        savedBook.price == 29.99

        and:
        Book foundBook = bookRepository.findById(savedBook.id).orElse(null)
        foundBook != null
        foundBook.title == "Test Book"
    }

    def "should find all books"() {
        given:
        Book book1 = createTestBook("Book 1", 19.99)
        Book book2 = createTestBook("Book 2", 24.99)
        bookRepository.save(book1)
        bookRepository.save(book2)

        when:
        List<Book> books = bookRepository.findAll()

        then:
        books.size() >= 2
        books*.title.containsAll(["Book 1", "Book 2"])
    }

    def "should update book"() {
        given:
        Book book = createTestBook("Original Title", 15.99)
        Book savedBook = bookRepository.save(book)

        when:
        savedBook.title = "Updated Title"
        savedBook.price = 19.99
        Book updatedBook = bookRepository.save(savedBook)

        then:
        updatedBook.title == "Updated Title"
        updatedBook.price == 19.99
    }

    def "should delete book"() {
        given:
        Book book = createTestBook("Book to Delete", 9.99)
        Book savedBook = bookRepository.save(book)

        when:
        bookRepository.deleteById(savedBook.id)

        then:
        bookRepository.findById(savedBook.id).isEmpty()
    }

    def "should find books with title filter"() {
        given:
        Book book1 = createTestBook("Java Programming", 39.99)
        Book book2 = createTestBook("Python Basics", 29.99)
        Book book3 = createTestBook("JavaScript Guide", 34.99)
        bookRepository.save(book1)
        bookRepository.save(book2)
        bookRepository.save(book3)

        Pageable pageable = PageRequest.of(0, 10)

        when:
        Page<Book> result = bookRepository.findBooksWithFilters("Java", null, null, pageable)

        then:
        result.content.size() == 2
        result.content*.title.containsAll(["Java Programming", "JavaScript Guide"])
    }

    def "should find books with author name filter"() {
        given:
        Author author1 = createTestAuthor("John Doe")
        Author author2 = createTestAuthor("Jane Smith")
        authorRepository.save(author1)
        authorRepository.save(author2)

        Book book1 = createTestBook("Book 1", 19.99)
        Book book2 = createTestBook("Book 2", 24.99)
        book1.authors.add(author1)
        book2.authors.add(author2)
        bookRepository.save(book1)
        bookRepository.save(book2)

        Pageable pageable = PageRequest.of(0, 10)

        when:
        Page<Book> result = bookRepository.findBooksWithFilters(null, "John", null, pageable)

        then:
        result.content.size() == 1
        result.content[0].title == "Book 1"
    }

    def "should find books with genre filter"() {
        given:
        Genre genre1 = createTestGenre("Fiction")
        Genre genre2 = createTestGenre("Science")
        genreRepository.save(genre1)
        genreRepository.save(genre2)

        Book book1 = createTestBook("Fiction Book", 19.99)
        Book book2 = createTestBook("Science Book", 24.99)
        book1.genres.add(genre1)
        book2.genres.add(genre2)
        bookRepository.save(book1)
        bookRepository.save(book2)

        Pageable pageable = PageRequest.of(0, 10)

        when:
        Page<Book> result = bookRepository.findBooksWithFilters(null, null, "Fiction", pageable)

        then:
        result.content.size() == 1
        result.content[0].title == "Fiction Book"
    }

    def "should find books with multiple filters"() {
        given:
        Author author = createTestAuthor("Test Author")
        Genre genre = createTestGenre("Test Genre")
        authorRepository.save(author)
        genreRepository.save(genre)

        Book matchingBook = createTestBook("Matching Book", 29.99)
        matchingBook.authors.add(author)
        matchingBook.genres.add(genre)

        Book nonMatchingBook = createTestBook("Non-Matching Book", 19.99)
        bookRepository.save(matchingBook)
        bookRepository.save(nonMatchingBook)

        Pageable pageable = PageRequest.of(0, 10)

        when:
        Page<Book> result = bookRepository.findBooksWithFilters("Matching", "Author", "Genre", pageable)

        then:
        result.content.size() == 1
        result.content[0].title == "Matching Book"
    }

    def "should return empty page when no books match filters"() {
        given:
        Book book = createTestBook("Test Book", 19.99)
        bookRepository.save(book)

        Pageable pageable = PageRequest.of(0, 10)

        when:
        Page<Book> result = bookRepository.findBooksWithFilters("NonExistent", null, null, pageable)

        then:
        result.content.isEmpty()
    }

    def "should handle case insensitive filtering"() {
        given:
        Book book = createTestBook("SPRING Boot Guide", 39.99)
        bookRepository.save(book)

        Pageable pageable = PageRequest.of(0, 10)

        when:
        Page<Book> result = bookRepository.findBooksWithFilters("spring", null, null, pageable)

        then:
        result.content.size() == 1
        result.content[0].title == "SPRING Boot Guide"
    }

    def "should handle pagination correctly"() {
        given:
        (1..5).each { i ->
            Book book = createTestBook("Book $i", 10.00 + i)
            bookRepository.save(book)
        }

        when:
        Page<Book> firstPage = bookRepository.findAll(PageRequest.of(0, 2))
        Page<Book> secondPage = bookRepository.findAll(PageRequest.of(1, 2))

        then:
        firstPage.content.size() == 2
        secondPage.content.size() == 2
        firstPage.totalElements == 5
        secondPage.totalElements == 5
    }

    def "should maintain relationships when saving and retrieving books"() {
        given:
        Author author = createTestAuthor("Test Author")
        Genre genre = createTestGenre("Test Genre")
        authorRepository.save(author)
        genreRepository.save(genre)

        Book book = createTestBook("Test Book", 29.99)
        book.authors.add(author)
        book.genres.add(genre)

        when:
        Book savedBook = bookRepository.save(book)
        Book retrievedBook = bookRepository.findById(savedBook.id).orElse(null)

        then:
        retrievedBook != null
        retrievedBook.authors.size() == 1
        retrievedBook.authors[0].name == "Test Author"
        retrievedBook.genres.size() == 1
        retrievedBook.genres[0].name == "Test Genre"
    }

    def "should handle books without relationships"() {
        given:
        Book book = createTestBook("Standalone Book", 15.99)

        when:
        Book savedBook = bookRepository.save(book)
        Book retrievedBook = bookRepository.findById(savedBook.id).orElse(null)

        then:
        retrievedBook != null
        retrievedBook.authors.isEmpty()
        retrievedBook.genres.isEmpty()
    }

    // Helper methods
    private Book createTestBook(String title, BigDecimal price) {
        return Book.builder()
                .title(title)
                .price(price)
                .authors(new ArrayList<>())
                .genres(new ArrayList<>())
                .build()
    }

    private Author createTestAuthor(String name) {
        return Author.builder()
                .name(name)
                .books(new ArrayList<>())
                .build()
    }

    private Genre createTestGenre(String name) {
        return Genre.builder()
                .name(name)
                .books(new ArrayList<>())
                .build()
    }
}