package com.verglor.azul.bookstore.controller

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.exception.ConflictException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.openapi.model.AuthorRequest
import com.verglor.azul.bookstore.openapi.model.AuthorResponse
import com.verglor.azul.bookstore.openapi.model.PagedResponse
import com.verglor.azul.bookstore.repository.AuthorRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class AuthorControllerDelegateSpec extends Specification {

    AuthorRepository authorRepository = Mock()

    @Subject
    AuthorControllerDelegate authorControllerDelegate = new AuthorControllerDelegate(authorRepository)

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
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 3
        response.body.content[0].name == "Author 1"
        response.body.content[1].name == "Author 2"
        response.body.content[2].name == "Author 3"
        response.body.page.totalElements == 3
        response.body.page.size == 10
        response.body.page.number == 0
    }

    def "should get author by id successfully"() {
        given:
        Long authorId = 1L
        Author author = createTestAuthor(authorId, "Test Author")

        when:
        authorRepository.findById(authorId) >> Optional.of(author)
        ResponseEntity<AuthorResponse> response = authorControllerDelegate.getAuthorById(authorId)

        then:
        response.statusCode == HttpStatus.OK
        response.body.id == authorId
        response.body.name == "Test Author"
    }

    def "should return not found when author doesn't exist"() {
        given:
        Long authorId = 999L

        when:
        authorRepository.findById(authorId) >> Optional.empty()
        authorControllerDelegate.getAuthorById(authorId)

        then:
        thrown(NotFoundException)
    }

    def "should create author successfully"() {
        given:
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("New Author")
                .build()

        Author savedAuthor = createTestAuthor(1L, "New Author")

        when:
        authorRepository.existsByNameIgnoreCase("New Author") >> false
        authorRepository.save(_) >> savedAuthor
        ResponseEntity<AuthorResponse> response = authorControllerDelegate.createAuthor(authorRequest)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.id == 1L
        response.body.name == "New Author"
        1 * authorRepository.save(_) >> { Author author ->
            assert author.name == "New Author"
            return savedAuthor
        }
    }

    def "should return conflict when creating author with existing name"() {
        given:
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("Existing Author")
                .build()

        when:
        authorRepository.existsByNameIgnoreCase("Existing Author") >> true
        authorControllerDelegate.createAuthor(authorRequest)

        then:
        thrown(ConflictException)
        0 * authorRepository.save(_)
    }

    def "should update author successfully"() {
        given:
        Long authorId = 1L
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("Updated Author Name")
                .build()

        Author existingAuthor = createTestAuthor(authorId, "Original Name")
        Author updatedAuthor = createTestAuthor(authorId, "Updated Author Name")

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorRepository.findByNameIgnoreCase("Updated Author Name") >> Optional.empty()
        authorRepository.save(_) >> updatedAuthor
        ResponseEntity<AuthorResponse> response = authorControllerDelegate.updateAuthor(authorId, authorRequest)

        then:
        response.statusCode == HttpStatus.OK
        response.body.id == authorId
        response.body.name == "Updated Author Name"
        1 * authorRepository.save(_) >> { Author author ->
            assert author.name == "Updated Author Name"
            return updatedAuthor
        }
    }

    def "should return conflict when updating author with existing name"() {
        given:
        Long authorId = 1L
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("Conflicting Name")
                .build()

        Author existingAuthor = createTestAuthor(authorId, "Original Name")
        Author conflictingAuthor = createTestAuthor(2L, "Conflicting Name")

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorRepository.findByNameIgnoreCase("Conflicting Name") >> Optional.of(conflictingAuthor)
        authorControllerDelegate.updateAuthor(authorId, authorRequest)

        then:
        thrown(ConflictException)
        0 * authorRepository.save(_)
    }

    def "should return not found when updating non-existent author"() {
        given:
        Long authorId = 999L
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("Updated Name")
                .build()

        when:
        authorRepository.findById(authorId) >> Optional.empty()
        authorControllerDelegate.updateAuthor(authorId, authorRequest)

        then:
        thrown(NotFoundException)
        0 * authorRepository.save(_)
    }

    def "should delete author successfully"() {
        given:
        Long authorId = 1L
        Author existingAuthor = createTestAuthor(authorId, "Author to Delete")
        existingAuthor.books = [] // No associated books

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        ResponseEntity<Void> response = authorControllerDelegate.deleteAuthor(authorId)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
        response.body == null
        1 * authorRepository.deleteById(authorId)
    }

    def "should return conflict when deleting author with associated books"() {
        given:
        Long authorId = 1L
        Author existingAuthor = createTestAuthor(authorId, "Author with Books")
        existingAuthor.books = [createTestBook(1L, "Book 1"), createTestBook(2L, "Book 2")] // Has associated books

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorControllerDelegate.deleteAuthor(authorId)

        then:
        thrown(ConflictException)
        0 * authorRepository.deleteById(_)
    }

    def "should return not found when deleting non-existent author"() {
        given:
        Long authorId = 999L

        when:
        authorRepository.findById(authorId) >> Optional.empty()
        authorControllerDelegate.deleteAuthor(authorId)

        then:
        thrown(NotFoundException)
        0 * authorRepository.deleteById(_)
    }

    def "should handle pagination metadata correctly"() {
        given:
        Pageable pageable = PageRequest.of(2, 3) // Third page, 3 items per page
        List<Author> authors = [createTestAuthor(7L, "Author 7")]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 10) // Total 10 authors

        when:
        authorRepository.findAll(pageable) >> authorPage
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.page.size == 3
        response.body.page.number == 2
        response.body.page.totalElements == 10
        response.body.page.totalPages == 4 // 10 items / 3 per page = 4 pages (rounded up)
    }

    def "should handle empty result set"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        Page<Author> emptyPage = new PageImpl<>([], pageable, 0)

        when:
        authorRepository.findAll(pageable) >> emptyPage
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 0
        response.body.page.totalElements == 0
        response.body.page.totalPages == 0
    }

    def "should handle case insensitive name conflict on create"() {
        given:
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("test author")
                .build()

        when:
        authorRepository.existsByNameIgnoreCase("test author") >> true
        authorControllerDelegate.createAuthor(authorRequest)

        then:
        thrown(ConflictException)
    }

    def "should handle case insensitive name conflict on update"() {
        given:
        Long authorId = 1L
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("TEST NAME")
                .build()

        Author existingAuthor = createTestAuthor(authorId, "Original Name")
        Author conflictingAuthor = createTestAuthor(2L, "test name")

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorRepository.findByNameIgnoreCase("TEST NAME") >> Optional.of(conflictingAuthor)
        authorControllerDelegate.updateAuthor(authorId, authorRequest)

        then:
        thrown(ConflictException)
    }

    def "should allow updating author to same name (case insensitive)"() {
        given:
        Long authorId = 1L
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("SAME NAME")
                .build()

        Author existingAuthor = createTestAuthor(authorId, "same name")
        Author updatedAuthor = createTestAuthor(authorId, "SAME NAME")

        when:
        authorRepository.findById(authorId) >> Optional.of(existingAuthor)
        authorRepository.findByNameIgnoreCase("SAME NAME") >> Optional.of(existingAuthor) // Same author
        authorRepository.save(_) >> updatedAuthor
        ResponseEntity<AuthorResponse> response = authorControllerDelegate.updateAuthor(authorId, authorRequest)

        then:
        response.statusCode == HttpStatus.OK
        response.body.name == "SAME NAME"
    }

    def "should search authors by name containing"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Author> authors = [
            createTestAuthor(1L, "Stephen King"),
            createTestAuthor(2L, "King Arthur")
        ]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 2)

        when:
        authorRepository.findByNameIgnoreCaseContaining("king", pageable) >> authorPage
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors("king", pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 2
        response.body.content[0].name == "Stephen King"
        response.body.content[1].name == "King Arthur"
        response.body.page.totalElements == 2
    }

    def "should search authors case insensitive"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Author> authors = [createTestAuthor(1L, "Stephen King")]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 1)

        when:
        authorRepository.findByNameIgnoreCaseContaining("KING", pageable) >> authorPage
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors("KING", pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Stephen King"
    }

    def "should return all authors when name is null"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Author> authors = [createTestAuthor(1L, "Author 1")]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 1)

        when:
        authorRepository.findAll(pageable) >> authorPage
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Author 1"
    }

    def "should return all authors when name is empty"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Author> authors = [createTestAuthor(1L, "Author 1")]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 1)

        when:
        authorRepository.findAll(pageable) >> authorPage
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors("", pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Author 1"
    }

    def "should return all authors when name is whitespace"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Author> authors = [createTestAuthor(1L, "Author 1")]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 1)

        when:
        authorRepository.findAll(pageable) >> authorPage
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors("   ", pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Author 1"
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
                .price(19.99)
                .authors([])
                .genres([])
                .build()
    }
}