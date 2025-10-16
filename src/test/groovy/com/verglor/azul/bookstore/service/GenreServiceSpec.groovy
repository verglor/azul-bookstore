package com.verglor.azul.bookstore.service

import com.verglor.azul.bookstore.domain.Genre
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.exception.ConflictException
import com.verglor.azul.bookstore.exception.NotFoundException
import com.verglor.azul.bookstore.repository.GenreRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification
import spock.lang.Subject

class GenreServiceSpec extends Specification {

    GenreRepository genreRepository = Mock()

    @Subject
    GenreServiceImpl genreService = new GenreServiceImpl(genreRepository)

    def "should get all genres with pagination"() {
        given:
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [
            createTestGenre(1L, "Genre 1"),
            createTestGenre(2L, "Genre 2"),
            createTestGenre(3L, "Genre 3")
        ]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 3)

        when:
        genreRepository.findAll(pageable) >> genrePage
        Page<Genre> result = genreService.getAllGenres(null, pageable)

        then:
        result == genrePage
        result.content.size() == 3
        result.content[0].name == "Genre 1"
        result.content[1].name == "Genre 2"
        result.content[2].name == "Genre 3"
    }

    def "should get all genres with name filter"() {
        given:
        String name = "Test"
        Pageable pageable = PageRequest.of(0, 10)
        List<Genre> genres = [createTestGenre(1L, "Test Genre")]
        Page<Genre> genrePage = new PageImpl<>(genres, pageable, 1)

        when:
        genreRepository.findByNameIgnoreCaseContaining(name.trim(), pageable) >> genrePage
        Page<Genre> result = genreService.getAllGenres(name, pageable)

        then:
        result == genrePage
        result.content.size() == 1
        result.content[0].name == "Test Genre"
    }

    def "should get genre by id successfully"() {
        given:
        Long genreId = 1L
        Genre genre = createTestGenre(genreId, "Test Genre")

        when:
        genreRepository.findById(genreId) >> Optional.of(genre)
        Genre result = genreService.getGenreById(genreId)

        then:
        result == genre
        result.id == genreId
        result.name == "Test Genre"
    }

    def "should throw NotFoundException when genre doesn't exist"() {
        given:
        Long genreId = 999L

        when:
        genreRepository.findById(genreId) >> Optional.empty()
        genreService.getGenreById(genreId)

        then:
        thrown(NotFoundException)
    }

    def "should create genre successfully"() {
        given:
        String name = "New Genre"
        Genre savedGenre = createTestGenre(1L, name)

        when:
        genreRepository.existsByNameIgnoreCase(name) >> false
        genreRepository.save(_) >> savedGenre
        Genre result = genreService.createGenre(name)

        then:
        result == savedGenre
        result.id == 1L
        result.name == "New Genre"
        1 * genreRepository.save(_) >> { Genre genre ->
            assert genre.name == "New Genre"
            return savedGenre
        }
    }

    def "should throw ConflictException when creating genre with existing name"() {
        given:
        String name = "Existing Genre"

        when:
        genreRepository.existsByNameIgnoreCase(name) >> true
        genreService.createGenre(name)

        then:
        thrown(ConflictException)
        0 * genreRepository.save(_)
    }

    def "should update genre successfully"() {
        given:
        Long genreId = 1L
        String newName = "Updated Genre Name"
        Genre existingGenre = createTestGenre(genreId, "Original Name")
        Genre updatedGenre = createTestGenre(genreId, newName)

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreRepository.findByNameIgnoreCase(newName) >> Optional.empty()
        genreRepository.save(_) >> updatedGenre
        Genre result = genreService.updateGenre(genreId, newName)

        then:
        result == updatedGenre
        result.id == genreId
        result.name == "Updated Genre Name"
        1 * genreRepository.save(_) >> { Genre genre ->
            assert genre.name == "Updated Genre Name"
            return updatedGenre
        }
    }

    def "should throw ConflictException when updating genre with existing name"() {
        given:
        Long genreId = 1L
        String newName = "Conflicting Name"
        Genre existingGenre = createTestGenre(genreId, "Original Name")
        Genre conflictingGenre = createTestGenre(2L, newName)

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreRepository.findByNameIgnoreCase(newName) >> Optional.of(conflictingGenre)
        genreService.updateGenre(genreId, newName)

        then:
        thrown(ConflictException)
        0 * genreRepository.save(_)
    }

    def "should throw NotFoundException when updating non-existent genre"() {
        given:
        Long genreId = 999L
        String newName = "Updated Name"

        when:
        genreRepository.findById(genreId) >> Optional.empty()
        genreService.updateGenre(genreId, newName)

        then:
        thrown(NotFoundException)
        0 * genreRepository.save(_)
    }

    def "should delete genre successfully when no associated books"() {
        given:
        Long genreId = 1L
        Genre existingGenre = createTestGenre(genreId, "Genre to Delete")
        existingGenre.books = [] // No associated books

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreService.deleteGenre(genreId)

        then:
        1 * genreRepository.deleteById(genreId)
    }

    def "should throw ConflictException when deleting genre with associated books"() {
        given:
        Long genreId = 1L
        Genre existingGenre = createTestGenre(genreId, "Genre with Books")
        existingGenre.books = [createTestBook(1L, "Book 1"), createTestBook(2L, "Book 2")] // Has associated books

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreService.deleteGenre(genreId)

        then:
        thrown(ConflictException)
        0 * genreRepository.deleteById(_)
    }

    def "should throw NotFoundException when deleting non-existent genre"() {
        given:
        Long genreId = 999L

        when:
        genreRepository.findById(genreId) >> Optional.empty()
        genreService.deleteGenre(genreId)

        then:
        thrown(NotFoundException)
        0 * genreRepository.deleteById(_)
    }

    def "should handle case insensitive name conflict on create"() {
        given:
        String name = "test genre"

        when:
        genreRepository.existsByNameIgnoreCase(name) >> true
        genreService.createGenre(name)

        then:
        thrown(ConflictException)
    }

    def "should handle case insensitive name conflict on update"() {
        given:
        Long genreId = 1L
        String newName = "TEST NAME"
        Genre existingGenre = createTestGenre(genreId, "Original Name")
        Genre conflictingGenre = createTestGenre(2L, "test name")

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreRepository.findByNameIgnoreCase(newName) >> Optional.of(conflictingGenre)
        genreService.updateGenre(genreId, newName)

        then:
        thrown(ConflictException)
    }

    def "should allow updating genre to same name (case insensitive)"() {
        given:
        Long genreId = 1L
        String newName = "SAME NAME"
        Genre existingGenre = createTestGenre(genreId, "same name")
        Genre updatedGenre = createTestGenre(genreId, newName)

        when:
        genreRepository.findById(genreId) >> Optional.of(existingGenre)
        genreRepository.findByNameIgnoreCase(newName) >> Optional.of(existingGenre) // Same genre
        genreRepository.save(_) >> updatedGenre
        Genre result = genreService.updateGenre(genreId, newName)

        then:
        result.name == "SAME NAME"
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
                .price(BigDecimal.valueOf(19.99))
                .authors([])
                .genres([])
                .build()
    }
}