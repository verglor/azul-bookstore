package com.verglor.azul.bookstore.controller

import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import com.verglor.azul.bookstore.exception.ConflictException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.openapi.model.GenreRequest
import com.verglor.azul.bookstore.openapi.model.GenreResponse
import com.verglor.azul.bookstore.openapi.model.PagedResponse
import com.verglor.azul.bookstore.repository.GenreRepository
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

    GenreRepository genreRepository = Mock()

    @Subject
    GenreControllerDelegate genreControllerDelegate = new GenreControllerDelegate(genreRepository)

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
        genreRepository.findAll(pageable) >> genrePage
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
        genreRepository.findById(genreId) >> Optional.of(genre)
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
        genreRepository.findById(genreId) >> Optional.empty()
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
        genreRepository.existsByNameIgnoreCase("New Genre") >> false
        genreRepository.save(_) >> savedGenre
        ResponseEntity<GenreResponse> response = genreControllerDelegate.createGenre(genreRequest)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.id == 1L
        response.body.name == "New Genre"
        1 * genreRepository.save(_) >> { Genre genre ->
            assert genre.name == "New Genre"
            return savedGenre
        }
    }

    def "should return conflict when creating genre with existing name"() {
        given:
        GenreRequest genreRequest = GenreRequest.builder()
                .name("Existing Genre")
                .build()

        when:
        genreRepository.existsByNameIgnoreCase("Existing Genre") >> true
        genreControllerDelegate.createGenre(genreRequest)

        then:
        thrown(ConflictException)
        0 * genreRepository.save(_)
    }

    def "should update genre successfully"() {
        given:
        Long genreId = 1L
        GenreRequest genreRequest = GenreRequest.builder()
                .name("Updated Genre Name")
                .build()

        Genre existingGenre = createTestGenre(genreId, "Original Name")
        Genre updatedGenre = createTestGenre(genreId, "Updated Genre Name")

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreRepository.findByNameIgnoreCase("Updated Genre Name") >> Optional.empty()
        genreRepository.save(_) >> updatedGenre
        ResponseEntity<GenreResponse> response = genreControllerDelegate.updateGenre(genreId, genreRequest)

        then:
        response.statusCode == HttpStatus.OK
        response.body.id == genreId
        response.body.name == "Updated Genre Name"
        1 * genreRepository.save(_) >> { Genre genre ->
            assert genre.name == "Updated Genre Name"
            return updatedGenre
        }
    }

    def "should return conflict when updating genre with existing name"() {
        given:
        Long genreId = 1L
        GenreRequest genreRequest = GenreRequest.builder()
                .name("Conflicting Name")
                .build()

        Genre existingGenre = createTestGenre(genreId, "Original Name")
        Genre conflictingGenre = createTestGenre(2L, "Conflicting Name")

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreRepository.findByNameIgnoreCase("Conflicting Name") >> Optional.of(conflictingGenre)
        genreControllerDelegate.updateGenre(genreId, genreRequest)

        then:
        thrown(ConflictException)
        0 * genreRepository.save(_)
    }

    def "should return not found when updating non-existent genre"() {
        given:
        Long genreId = 999L
        GenreRequest genreRequest = GenreRequest.builder()
                .name("Updated Name")
                .build()

        when:
        genreRepository.findById(genreId) >> Optional.empty()
        genreControllerDelegate.updateGenre(genreId, genreRequest)

        then:
        thrown(NotFoundException)
        0 * genreRepository.save(_)
    }

    def "should delete genre successfully"() {
        given:
        Long genreId = 1L
        Genre existingGenre = createTestGenre(genreId, "Genre to Delete")
        existingGenre.books = [] // No associated books

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        ResponseEntity<Void> response = genreControllerDelegate.deleteGenre(genreId)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
        response.body == null
        1 * genreRepository.deleteById(genreId)
    }

    def "should return conflict when deleting genre with associated books"() {
        given:
        Long genreId = 1L
        Genre existingGenre = createTestGenre(genreId, "Genre with Books")
        existingGenre.books = [createTestBook(1L, "Book 1"), createTestBook(2L, "Book 2")] // Has associated books

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreControllerDelegate.deleteGenre(genreId)

        then:
        thrown(ConflictException)
        0 * genreRepository.deleteById(_)
    }

    def "should return not found when deleting non-existent genre"() {
        given:
        Long genreId = 999L

        when:
        genreRepository.findById(genreId) >> Optional.empty()
        genreControllerDelegate.deleteGenre(genreId)

        then:
        thrown(NotFoundException)
        0 * genreRepository.deleteById(_)
    }

    def "should handle pagination metadata correctly"() {
        given:
        Pageable pageable = PageRequest.of(1, 5) // Second page, 5 items per page
        List<Genre> genres = [createTestGenre(6L, "Genre 6")]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 12) // Total 12 genres

        when:
        genreRepository.findAll(pageable) >> genrePage
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
        genreRepository.findAll(pageable) >> emptyPage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 0
        response.body.page.totalElements == 0
        response.body.page.totalPages == 0
    }

    def "should handle case insensitive name conflict on create"() {
        given:
        GenreRequest genreRequest = GenreRequest.builder()
                .name("test genre")
                .build()

        when:
        genreRepository.existsByNameIgnoreCase("test genre") >> true
        genreControllerDelegate.createGenre(genreRequest)

        then:
        thrown(ConflictException)
    }

    def "should handle case insensitive name conflict on update"() {
        given:
        Long genreId = 1L
        GenreRequest genreRequest = GenreRequest.builder()
                .name("TEST NAME")
                .build()

        Genre existingGenre = createTestGenre(genreId, "Original Name")
        Genre conflictingGenre = createTestGenre(2L, "test name")

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreRepository.findByNameIgnoreCase("TEST NAME") >> Optional.of(conflictingGenre)
        genreControllerDelegate.updateGenre(genreId, genreRequest)

        then:
        thrown(ConflictException)
    }

    def "should allow updating genre to same name (case insensitive)"() {
        given:
        Long genreId = 1L
        GenreRequest genreRequest = GenreRequest.builder()
                .name("SAME NAME")
                .build()

        Genre existingGenre = createTestGenre(genreId, "same name")
        Genre updatedGenre = createTestGenre(genreId, "SAME NAME")

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreRepository.findByNameIgnoreCase("SAME NAME") >> Optional.of(existingGenre) // Same genre
        genreRepository.save(_) >> updatedGenre
        ResponseEntity<GenreResponse> response = genreControllerDelegate.updateGenre(genreId, genreRequest)

        then:
        response.statusCode == HttpStatus.OK
        response.body.name == "SAME NAME"
    }

    def "should handle large page numbers correctly"() {
        given:
        Pageable pageable = PageRequest.of(10, 10) // Page 10, 10 items per page
        List<Genre> genres = []
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 105) // Total 105 genres

        when:
        genreRepository.findAll(pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 0 // Empty because we're on page 10 of 10 total pages (105/10 = 10.5, so 11 pages)
        response.body.page.totalElements == 105
        response.body.page.totalPages == 11
        response.body.page.number == 10
    }

    def "should handle single item result set"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [createTestGenre(1L, "Only Genre")]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 1)

        when:
        genreRepository.findAll(pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Only Genre"
        response.body.page.totalElements == 1
        response.body.page.totalPages == 1
    }

    def "should handle genre name with unicode characters"() {
        given:
        GenreRequest genreRequest = GenreRequest.builder()
                .name("文学 (Literature)")
                .build()

        Genre savedGenre = createTestGenre(1L, "文学 (Literature)")

        when:
        genreRepository.existsByNameIgnoreCase("文学 (Literature)") >> false
        genreRepository.save(_) >> savedGenre
        ResponseEntity<GenreResponse> response = genreControllerDelegate.createGenre(genreRequest)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.id == 1L
        response.body.name == "文学 (Literature)"
    }

    def "should search genres by name containing"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [
            createTestGenre(1L, "Science Fiction"),
            createTestGenre(2L, "Fiction")
        ]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 2)

        when:
        genreRepository.findByNameIgnoreCaseContaining("fiction", pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres("fiction", pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 2
        response.body.content[0].name == "Science Fiction"
        response.body.content[1].name == "Fiction"
        response.body.page.totalElements == 2
    }

    def "should search genres case insensitive"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [createTestGenre(1L, "Science Fiction")]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 1)

        when:
        genreRepository.findByNameIgnoreCaseContaining("FICTION", pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres("FICTION", pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Science Fiction"
    }

    def "should return all genres when name is null"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [createTestGenre(1L, "Genre 1")]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 1)

        when:
        genreRepository.findAll(pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres(null, pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Genre 1"
    }

    def "should return all genres when name is empty"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [createTestGenre(1L, "Genre 1")]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 1)

        when:
        genreRepository.findAll(pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres("", pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Genre 1"
    }

    def "should return all genres when name is whitespace"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [createTestGenre(1L, "Genre 1")]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 1)

        when:
        genreRepository.findAll(pageable) >> genrePage
        ResponseEntity<PagedResponse> response = genreControllerDelegate.getAllGenres("   ", pageable)

        then:
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 1
        response.body.content[0].name == "Genre 1"
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