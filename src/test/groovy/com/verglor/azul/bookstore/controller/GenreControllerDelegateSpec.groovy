package com.verglor.azul.bookstore.controller

import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import com.verglor.azul.bookstore.exception.ConflictException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.openapi.model.GenreRequest
import com.verglor.azul.bookstore.openapi.model.GenreResponse
import com.verglor.azul.bookstore.openapi.model.PagedResponse
import com.verglor.azul.bookstore.service.GenreService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject
import static io.restassured.RestAssured.*
import static org.hamcrest.Matchers.*

class GenreControllerDelegateSpec extends Specification {

    GenreService genreService = Mock()

    @Subject
    GenreControllerDelegate genreControllerDelegate = new GenreControllerDelegate(genreService)

    def "should get all genres with pagination"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [
            createTestGenre(1L, "Fiction"),
            createTestGenre(2L, "Science"),
            createTestGenre(3L, "History")
        ]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 3)

        when:
        genreService.getAllGenres(null, pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 3
        response.body.content[0].name == "Fiction"
        response.body.content[1].name == "Science"
        response.body.content[2].name == "History"
        response.body.page.totalElements == 3
        response.body.page.size == 10
        response.body.page.number == 0
    }

    def "should get genre by id successfully"() {
        given:
        Long genreId = 1L
        Genre genre = createTestGenre(genreId, "Test Genre")

        when:
        genreService.getGenreById(genreId) >> genre
        ResponseEntity<GenreResponse> response = genreControllerDelegate.getGenreById(genreId)

        then:
        response.statusCode == HttpStatus.OK
        response.body.id == genreId
        response.body.name == "Test Genre"
    }

    def "should return not found when genre doesn't exist"() {
        given:
        Long genreId = 999L

        when:
        genreService.getGenreById(genreId) >> { throw new NotFoundException("Genre not found") }
        genreControllerDelegate.getGenreById(genreId)

        then:
        thrown(NotFoundException)
    }

    def "should create genre successfully"() {
        given:
        GenreRequest genreRequest = GenreRequest.builder()
                .name("New Genre")
                .build()

        Genre savedGenre = createTestGenre(1L, "New Genre")

        when:
        genreService.createGenre("New Genre") >> savedGenre
        ResponseEntity<GenreResponse> response = genreControllerDelegate.createGenre(genreRequest)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.id == 1L
        response.body.name == "New Genre"
    }

    def "should return conflict when creating genre with existing name"() {
        given:
        GenreRequest genreRequest = GenreRequest.builder()
                .name("Existing Genre")
                .build()

        when:
        genreService.createGenre("Existing Genre") >> { throw new ConflictException("Genre already exists") }
        genreControllerDelegate.createGenre(genreRequest)

        then:
        thrown(ConflictException)
    }

    def "should update genre successfully"() {
        given:
        Long genreId = 1L
        GenreRequest genreRequest = GenreRequest.builder()
                .name("Updated Genre Name")
                .build()

        Genre updatedGenre = createTestGenre(genreId, "Updated Genre Name")

        when:
        genreService.updateGenre(genreId, "Updated Genre Name") >> updatedGenre
        ResponseEntity<GenreResponse> response = genreControllerDelegate.updateGenre(genreId, genreRequest)

        then:
        response.statusCode == HttpStatus.OK
        response.body.id == genreId
        response.body.name == "Updated Genre Name"
    }

    def "should return conflict when updating genre with existing name"() {
        given:
        Long genreId = 1L
        GenreRequest genreRequest = GenreRequest.builder()
                .name("Conflicting Name")
                .build()

        when:
        genreService.updateGenre(genreId, "Conflicting Name") >> { throw new ConflictException("Genre name already exists") }
        genreControllerDelegate.updateGenre(genreId, genreRequest)

        then:
        thrown(ConflictException)
    }

    def "should return not found when updating non-existent genre"() {
        given:
        Long genreId = 999L
        GenreRequest genreRequest = GenreRequest.builder()
                .name("Updated Name")
                .build()

        when:
        genreService.updateGenre(genreId, "Updated Name") >> { throw new NotFoundException("Genre not found") }
        genreControllerDelegate.updateGenre(genreId, genreRequest)

        then:
        thrown(NotFoundException)
    }

    def "should delete genre successfully"() {
        given:
        Long genreId = 1L

        when:
        genreService.deleteGenre(genreId) >> {}
        ResponseEntity<Void> response = genreControllerDelegate.deleteGenre(genreId)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
        response.body == null
    }

    def "should return conflict when deleting genre with associated books"() {
        given:
        Long genreId = 1L

        when:
        genreService.deleteGenre(genreId) >> { throw new ConflictException("Cannot delete genre with associated books") }
        genreControllerDelegate.deleteGenre(genreId)

        then:
        thrown(ConflictException)
    }

    def "should return not found when deleting non-existent genre"() {
        given:
        Long genreId = 999L

        when:
        genreService.deleteGenre(genreId) >> { throw new NotFoundException("Genre not found") }
        genreControllerDelegate.deleteGenre(genreId)

        then:
        thrown(NotFoundException)
    }

    def "should handle pagination metadata correctly"() {
        given:
        Pageable pageable = PageRequest.of(1, 5) // Second page, 5 items per page
        List<Genre> genres = [createTestGenre(6L, "Genre 6")]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 12) // Total 12 genres

        when:
        genreService.getAllGenres(null, pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.page.size == 5
        response.body.page.number == 1
        response.body.page.totalElements == 12
        response.body.page.totalPages == 3 // 12 items / 5 per page = 3 pages (rounded up)
    }

    def "should handle empty result set"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        Page<Genre> emptyPage = new PageImpl<>([], pageable, 0)

        when:
        genreService.getAllGenres(null, pageable) >> emptyPage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 0
        response.body.page.totalElements == 0
        response.body.page.totalPages == 0
    }


    // Helper methods
    private Genre createTestGenre(Long id, String name) {
        return Genre.builder()
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