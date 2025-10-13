package com.verglor.azul.bookstore.repository

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorRepositoryIntegrationSpec extends Specification {

    @Autowired
    AuthorRepository authorRepository

    @Autowired
    BookRepository bookRepository

    def setup() {
        // Clean up database before each test
        authorRepository.deleteAll()
        bookRepository.deleteAll()
    }

    def "should save and find author by id"() {
        given:
        Author author = createTestAuthor("John Doe")

        when:
        Author savedAuthor = authorRepository.save(author)

        then:
        savedAuthor.id != null
        savedAuthor.name == "John Doe"

        and:
        Author foundAuthor = authorRepository.findById(savedAuthor.id).orElse(null)
        foundAuthor != null
        foundAuthor.name == "John Doe"
    }

    def "should find all authors"() {
        given:
        Author author1 = createTestAuthor("Author One")
        Author author2 = createTestAuthor("Author Two")
        authorRepository.save(author1)
        authorRepository.save(author2)

        when:
        List<Author> authors = authorRepository.findAll()

        then:
        authors.size() >= 2
        authors*.name.containsAll(["Author One", "Author Two"])
    }

    def "should update author"() {
        given:
        Author author = createTestAuthor("Original Name")
        Author savedAuthor = authorRepository.save(author)

        when:
        savedAuthor.name = "Updated Name"
        Author updatedAuthor = authorRepository.save(savedAuthor)

        then:
        updatedAuthor.name == "Updated Name"
    }

    def "should delete author"() {
        given:
        Author author = createTestAuthor("Author to Delete")
        Author savedAuthor = authorRepository.save(author)

        when:
        authorRepository.deleteById(savedAuthor.id)

        then:
        authorRepository.findById(savedAuthor.id).isEmpty()
    }

    def "should check if author exists by name case insensitive"() {
        given:
        Author author = createTestAuthor("Stephen King")
        authorRepository.save(author)

        when:
        boolean existsLowercase = authorRepository.existsByNameIgnoreCase("stephen king")
        boolean existsUppercase = authorRepository.existsByNameIgnoreCase("STEPHEN KING")
        boolean existsMixedCase = authorRepository.existsByNameIgnoreCase("Stephen King")
        boolean notExists = authorRepository.existsByNameIgnoreCase("Non Existent Author")

        then:
        existsLowercase
        existsUppercase
        existsMixedCase
        !notExists
    }

    def "should find author by name case insensitive"() {
        given:
        Author author = createTestAuthor("Agatha Christie")
        authorRepository.save(author)

        when:
        Optional<Author> foundLowercase = authorRepository.findByNameIgnoreCase("agatha christie")
        Optional<Author> foundUppercase = authorRepository.findByNameIgnoreCase("AGATHA CHRISTIE")
        Optional<Author> foundMixedCase = authorRepository.findByNameIgnoreCase("Agatha Christie")
        Optional<Author> notFound = authorRepository.findByNameIgnoreCase("Non Existent Author")

        then:
        foundLowercase.isPresent()
        foundLowercase.get().name == "Agatha Christie"

        foundUppercase.isPresent()
        foundUppercase.get().name == "Agatha Christie"

        foundMixedCase.isPresent()
        foundMixedCase.get().name == "Agatha Christie"

        notFound.isEmpty()
    }

    def "should handle authors with exact name matches correctly"() {
        given:
        Author author1 = createTestAuthor("John Smith")
        Author author2 = createTestAuthor("Jane Smith")
        authorRepository.save(author1)
        authorRepository.save(author2)

        when:
        boolean existsJohnSmith = authorRepository.existsByNameIgnoreCase("john smith")
        boolean existsJaneSmith = authorRepository.existsByNameIgnoreCase("jane smith")
        Optional<Author> foundJohn = authorRepository.findByNameIgnoreCase("john smith")

        then:
        existsJohnSmith
        existsJaneSmith
        foundJohn.isPresent()
        foundJohn.get().name == "John Smith"
    }

    def "should maintain bidirectional relationship with books"() {
        given:
        Author author = createTestAuthor("Test Author")
        Book book1 = createTestBook("Book 1", 19.99)
        Book book2 = createTestBook("Book 2", 24.99)

        when:
        Author savedAuthor = authorRepository.save(author)
        book1.addAuthor(savedAuthor)  // Use helper method for proper bidirectional relationship
        book2.addAuthor(savedAuthor)
        bookRepository.save(book1)
        bookRepository.save(book2)

        // Retrieve author with books
        Author retrievedAuthor = authorRepository.findById(savedAuthor.id).orElse(null)

        then:
        retrievedAuthor != null
        retrievedAuthor.books.size() == 2
        retrievedAuthor.books*.title.containsAll(["Book 1", "Book 2"])
    }

    def "should handle author without books"() {
        given:
        Author author = createTestAuthor("Standalone Author")

        when:
        Author savedAuthor = authorRepository.save(author)
        Author retrievedAuthor = authorRepository.findById(savedAuthor.id).orElse(null)

        then:
        retrievedAuthor != null
        retrievedAuthor.books.isEmpty()
    }

    def "should handle authors with different names"() {
        given:
        Author author1 = createTestAuthor("Mark Twain")
        Author author2 = createTestAuthor("Stephen King")
        authorRepository.save(author1)
        authorRepository.save(author2)

        when:
        boolean existsMark = authorRepository.existsByNameIgnoreCase("mark twain")
        boolean existsStephen = authorRepository.existsByNameIgnoreCase("stephen king")

        then:
        existsMark
        existsStephen
        authorRepository.count() == 2
    }

    def "should handle empty and null name checks"() {
        given:
        Author author = createTestAuthor("Valid Author")
        authorRepository.save(author)

        when:
        boolean existsEmpty = authorRepository.existsByNameIgnoreCase("")
        boolean existsNull = authorRepository.existsByNameIgnoreCase(null)
        Optional<Author> foundEmpty = authorRepository.findByNameIgnoreCase("")
        Optional<Author> foundNull = authorRepository.findByNameIgnoreCase(null)

        then:
        !existsEmpty
        !existsNull
        foundEmpty.isEmpty()
        foundNull.isEmpty()
    }

    def "should handle special characters in author names"() {
        given:
        Author author = createTestAuthor("José María García-López")

        when:
        Author savedAuthor = authorRepository.save(author)
        Optional<Author> foundAuthor = authorRepository.findByNameIgnoreCase("josé maría garcía-lópez")

        then:
        savedAuthor != null
        foundAuthor.isPresent()
        foundAuthor.get().name == "José María García-López"
    }

    def "should handle very long author names"() {
        given:
        String longName = "A" * 100  // 100 character name
        Author author = createTestAuthor(longName)

        when:
        Author savedAuthor = authorRepository.save(author)
        Optional<Author> foundAuthor = authorRepository.findByNameIgnoreCase(longName.toLowerCase())

        then:
        savedAuthor != null
        foundAuthor.isPresent()
        foundAuthor.get().name == longName
    }

    // Helper methods
    private Author createTestAuthor(String name) {
        return Author.builder()
                .name(name)
                .books(new ArrayList<>())
                .build()
    }

    private Book createTestBook(String title, BigDecimal price) {
        return Book.builder()
                .title(title)
                .price(price)
                .authors(new ArrayList<>())
                .genres(new ArrayList<>())
                .build()
    }
}