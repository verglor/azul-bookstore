package com.verglor.azul.bookstore.controller

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.exception.ConflictException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.openapi.model.AuthorRequest
import com.verglor.azul.bookstore.openapi.model.AuthorResponse
import com.verglor.azul.bookstore.openapi.model.PagedResponse
import com.verglor.azul.bookstore.service.AuthorService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class AuthorControllerDelegateSpec extends Specification {

    AuthorService authorService = Mock()

    @Subject
    AuthorControllerDelegate authorControllerDelegate = new AuthorControllerDelegate(authorService)

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
        authorService.getAllAuthors(null, pageable) >> authorPage
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
        authorService.getAuthorById(authorId) >> author
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
        authorService.getAuthorById(authorId) >> { throw new NotFoundException("Author not found") }
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
        authorService.createAuthor("New Author") >> savedAuthor
        ResponseEntity<AuthorResponse> response = authorControllerDelegate.createAuthor(authorRequest)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.id == 1L
        response.body.name == "New Author"
    }

    def "should return conflict when creating author with existing name"() {
        given:
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("Existing Author")
                .build()

        when:
        authorService.createAuthor("Existing Author") >> { throw new ConflictException("Author already exists") }
        authorControllerDelegate.createAuthor(authorRequest)

        then:
        thrown(ConflictException)
    }

    def "should update author successfully"() {
        given:
        Long authorId = 1L
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("Updated Author Name")
                .build()

        Author updatedAuthor = createTestAuthor(authorId, "Updated Author Name")

        when:
        authorService.updateAuthor(authorId, "Updated Author Name") >> updatedAuthor
        ResponseEntity<AuthorResponse> response = authorControllerDelegate.updateAuthor(authorId, authorRequest)

        then:
        response.statusCode == HttpStatus.OK
        response.body.id == authorId
        response.body.name == "Updated Author Name"
    }

    def "should return conflict when updating author with existing name"() {
        given:
        Long authorId = 1L
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("Conflicting Name")
                .build()

        when:
        authorService.updateAuthor(authorId, "Conflicting Name") >> { throw new ConflictException("Author name already exists") }
        authorControllerDelegate.updateAuthor(authorId, authorRequest)

        then:
        thrown(ConflictException)
    }

    def "should return not found when updating non-existent author"() {
        given:
        Long authorId = 999L
        AuthorRequest authorRequest = AuthorRequest.builder()
                .name("Updated Name")
                .build()

        when:
        authorService.updateAuthor(authorId, "Updated Name") >> { throw new NotFoundException("Author not found") }
        authorControllerDelegate.updateAuthor(authorId, authorRequest)

        then:
        thrown(NotFoundException)
    }

    def "should delete author successfully"() {
        given:
        Long authorId = 1L

        when:
        authorService.deleteAuthor(authorId) >> {}
        ResponseEntity<Void> response = authorControllerDelegate.deleteAuthor(authorId)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
        response.body == null
    }

    def "should return conflict when deleting author with associated books"() {
        given:
        Long authorId = 1L

        when:
        authorService.deleteAuthor(authorId) >> { throw new ConflictException("Cannot delete author with associated books") }
        authorControllerDelegate.deleteAuthor(authorId)

        then:
        thrown(ConflictException)
    }

    def "should return not found when deleting non-existent author"() {
        given:
        Long authorId = 999L

        when:
        authorService.deleteAuthor(authorId) >> { throw new NotFoundException("Author not found") }
        authorControllerDelegate.deleteAuthor(authorId)

        then:
        thrown(NotFoundException)
    }

    def "should handle pagination metadata correctly"() {
        given:
        Pageable pageable = PageRequest.of(2, 3) // Third page, 3 items per page
        List<Author> authors = [createTestAuthor(7L, "Author 7")]
        Page<Author> authorPage = new PageImpl<>(authors, pageable, 10) // Total 10 authors

        when:
        authorService.getAllAuthors(null, pageable) >> authorPage
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
        authorService.getAllAuthors(null, pageable) >> emptyPage
        ResponseEntity<PagedResponse> response = authorControllerDelegate.getAllAuthors(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 0
        response.body.page.totalElements == 0
        response.body.page.totalPages == 0
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