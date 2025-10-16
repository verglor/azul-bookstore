package com.verglor.azul.bookstore.service

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.exception.ConflictException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.repository.AuthorRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification
import spock.lang.Subject

class AuthorServiceSpec extends Specification {

    AuthorRepository authorRepository = Mock()

    @Subject
    AuthorServiceImpl authorService = new AuthorServiceImpl(authorRepository)

    def "should get all authors with pagination"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Author> authors = [
            createTestAuthor(1L, "Author 1"),
            createTestAuthor(2L, "Author 2"),
            createTestAuthor(3L, "Author 3")
        ]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 3)

        when:
        authorRepository.findAll(pageable) >> authorPage
        Page<Author> result = authorService.getAllAuthors(null, pageable)

        then:
        result == authorPage
        result.content.size() == 3
        result.content[0].name == "Author 1"
        result.content[1].name == "Author 2"
        result.content[2].name == "Author 3"
    }

    def "should get all authors with name filter"() {
        given:
        String name = "Test"
        Pageable pageable = PageRequest.of(0, 10)
        List<Author> authors = [createTestAuthor(1L, "Test Author")]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 1)

        when:
        authorRepository.findByNameIgnoreCaseContaining(name.trim(), pageable) >> authorPage
        Page<Author> result = authorService.getAllAuthors(name, pageable)

        then:
        result == authorPage
        result.content.size() == 1
        result.content[0].name == "Test Author"
    }

    def "should get author by id successfully"() {
        given:
        Long authorId = 1L
        Author author = createTestAuthor(authorId, "Test Author")

        when:
        authorRepository.findById(authorId) >> Optional.of(author)
        Author result = authorService.getAuthorById(authorId)

        then:
        result == author
        result.id == authorId
        result.name == "Test Author"
    }

    def "should throw NotFoundException when author doesn't exist"() {
        given:
        Long authorId = 999L

        when:
        authorRepository.findById(authorId) >> Optional.empty()
        authorService.getAuthorById(authorId)

        then:
        thrown(NotFoundException)
    }

    def "should create author successfully"() {
        given:
        String name = "New Author"
        Author savedAuthor = createTestAuthor(1L, name)

        when:
        authorRepository.existsByNameIgnoreCase(name) >> false
        authorRepository.save(_) >> savedAuthor
        Author result = authorService.createAuthor(name)

        then:
        result == savedAuthor
        result.id == 1L
        result.name == "New Author"
        1 * authorRepository.save(_) >> { Author author ->
            assert author.name == "New Author"
            return savedAuthor
        }
    }

    def "should throw ConflictException when creating author with existing name"() {
        given:
        String name = "Existing Author"

        when:
        authorRepository.existsByNameIgnoreCase(name) >> true
        authorService.createAuthor(name)

        then:
        thrown(ConflictException)
        0 * authorRepository.save(_)
    }

    def "should update author successfully"() {
        given:
        Long authorId = 1L
        String newName = "Updated Author Name"
        Author existingAuthor = createTestAuthor(authorId, "Original Name")
        Author updatedAuthor = createTestAuthor(authorId, newName)

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorRepository.findByNameIgnoreCase(newName) >> Optional.empty()
        authorRepository.save(_) >> updatedAuthor
        Author result = authorService.updateAuthor(authorId, newName)

        then:
        result == updatedAuthor
        result.id == authorId
        result.name == "Updated Author Name"
        1 * authorRepository.save(_) >> { Author author ->
            assert author.name == "Updated Author Name"
            return updatedAuthor
        }
    }

    def "should throw ConflictException when updating author with existing name"() {
        given:
        Long authorId = 1L
        String newName = "Conflicting Name"
        Author existingAuthor = createTestAuthor(authorId, "Original Name")
        Author conflictingAuthor = createTestAuthor(2L, newName)

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorRepository.findByNameIgnoreCase(newName) >> Optional.of(conflictingAuthor)
        authorService.updateAuthor(authorId, newName)

        then:
        thrown(ConflictException)
        0 * authorRepository.save(_)
    }

    def "should throw NotFoundException when updating non-existent author"() {
        given:
        Long authorId = 999L
        String newName = "Updated Name"

        when:
        authorRepository.findById(authorId) >> Optional.empty()
        authorService.updateAuthor(authorId, newName)

        then:
        thrown(NotFoundException)
        0 * authorRepository.save(_)
    }

    def "should delete author successfully when no associated books"() {
        given:
        Long authorId = 1L
        Author existingAuthor = createTestAuthor(authorId, "Author to Delete")
        existingAuthor.books = [] // No associated books

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorService.deleteAuthor(authorId)

        then:
        1 * authorRepository.deleteById(authorId)
    }

    def "should throw ConflictException when deleting author with associated books"() {
        given:
        Long authorId = 1L
        Author existingAuthor = createTestAuthor(authorId, "Author with Books")
        existingAuthor.books = [createTestBook(1L, "Book 1"), createTestBook(2L, "Book 2")] // Has associated books

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorService.deleteAuthor(authorId)

        then:
        thrown(ConflictException)
        0 * authorRepository.deleteById(_)
    }

    def "should throw NotFoundException when deleting non-existent author"() {
        given:
        Long authorId = 999L

        when:
        authorRepository.findById(authorId) >> Optional.empty()
        authorService.deleteAuthor(authorId)

        then:
        thrown(NotFoundException)
        0 * authorRepository.deleteById(_)
    }

    def "should handle case insensitive name conflict on create"() {
        given:
        String name = "test author"

        when:
        authorRepository.existsByNameIgnoreCase(name) >> true
        authorService.createAuthor(name)

        then:
        thrown(ConflictException)
    }

    def "should handle case insensitive name conflict on update"() {
        given:
        Long authorId = 1L
        String newName = "TEST NAME"
        Author existingAuthor = createTestAuthor(authorId, "Original Name")
        Author conflictingAuthor = createTestAuthor(2L, "test name")

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorRepository.findByNameIgnoreCase(newName) >> Optional.of(conflictingAuthor)
        authorService.updateAuthor(authorId, newName)

        then:
        thrown(ConflictException)
    }

    def "should allow updating author to same name (case insensitive)"() {
        given:
        Long authorId = 1L
        String newName = "SAME NAME"
        Author existingAuthor = createTestAuthor(authorId, "same name")
        Author updatedAuthor = createTestAuthor(authorId, newName)

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorRepository.findByNameIgnoreCase(newName) >> Optional.of(existingAuthor) // Same author
        authorRepository.save(_) >> updatedAuthor
        Author result = authorService.updateAuthor(authorId, newName)

        then:
        result.name == "SAME NAME"
    }

    // Helper methods
    private Author createTestAuthor(Long id, String name) {
        return Author.builder()
                .id(id)
                .name(name)
                .books([])
                .build()
    }

    private Book createTestBook(Long id, String title) {
        return Book.builder()
                .id(id)
                .title(title)
                .price(BigDecimal.valueOf(19.99))
                .authors([])
                .genres([])
                .build()
    }
}