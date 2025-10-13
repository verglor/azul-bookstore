package com.verglor.azul.bookstore.repository

import com.verglor.azul.bookstore.domain.Author
import com.verglor.azul.bookstore.domain.Book
import com.verglor.azul.bookstore.domain.Genre
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GenreRepositoryIntegrationSpec extends Specification {

    @Autowired
    GenreRepository genreRepository

    @Autowired
    BookRepository bookRepository

    def setup() {
        // Clean up database before each test
        genreRepository.deleteAll()
        bookRepository.deleteAll()
    }

    def "should save and find genre by id"() {
        given:
        Genre genre = createTestGenre("Science Fiction")

        when:
        Genre savedGenre = genreRepository.save(genre)

        then:
        savedGenre.id != null
        savedGenre.name == "Science Fiction"

        and:
        Genre foundGenre = genreRepository.findById(savedGenre.id).orElse(null)
        foundGenre != null
        foundGenre.name == "Science Fiction"
    }

    def "should find all genres"() {
        given:
        Genre genre1 = createTestGenre("Fantasy")
        Genre genre2 = createTestGenre("Mystery")
        genreRepository.save(genre1)
        genreRepository.save(genre2)

        when:
        List<Genre> genres = genreRepository.findAll()

        then:
        genres.size() >= 2
        genres*.name.containsAll(["Fantasy", "Mystery"])
    }

    def "should update genre"() {
        given:
        Genre genre = createTestGenre("Original Genre")
        Genre savedGenre = genreRepository.save(genre)

        when:
        savedGenre.name = "Updated Genre"
        Genre updatedGenre = genreRepository.save(savedGenre)

        then:
        updatedGenre.name == "Updated Genre"
    }

    def "should delete genre"() {
        given:
        Genre genre = createTestGenre("Genre to Delete")
        Genre savedGenre = genreRepository.save(genre)

        when:
        genreRepository.deleteById(savedGenre.id)

        then:
        genreRepository.findById(savedGenre.id).isEmpty()
    }

    def "should check if genre exists by name case insensitive"() {
        given:
        Genre genre = createTestGenre("Romance")
        genreRepository.save(genre)

        when:
        boolean existsLowercase = genreRepository.existsByNameIgnoreCase("romance")
        boolean existsUppercase = genreRepository.existsByNameIgnoreCase("ROMANCE")
        boolean existsMixedCase = genreRepository.existsByNameIgnoreCase("Romance")
        boolean notExists = genreRepository.existsByNameIgnoreCase("Non Existent Genre")

        then:
        existsLowercase
        existsUppercase
        existsMixedCase
        !notExists
    }

    def "should find genre by name case insensitive"() {
        given:
        Genre genre = createTestGenre("Thriller")
        genreRepository.save(genre)

        when:
        Optional<Genre> foundLowercase = genreRepository.findByNameIgnoreCase("thriller")
        Optional<Genre> foundUppercase = genreRepository.findByNameIgnoreCase("THRILLER")
        Optional<Genre> foundMixedCase = genreRepository.findByNameIgnoreCase("Thriller")
        Optional<Genre> notFound = genreRepository.findByNameIgnoreCase("Non Existent Genre")

        then:
        foundLowercase.isPresent()
        foundLowercase.get().name == "Thriller"

        foundUppercase.isPresent()
        foundUppercase.get().name == "Thriller"

        foundMixedCase.isPresent()
        foundMixedCase.get().name == "Thriller"

        notFound.isEmpty()
    }

    def "should handle genres with exact name matches correctly"() {
        given:
        Genre genre1 = createTestGenre("Historical Fiction")
        Genre genre2 = createTestGenre("Science Fiction")
        genreRepository.save(genre1)
        genreRepository.save(genre2)

        when:
        boolean existsHistorical = genreRepository.existsByNameIgnoreCase("historical fiction")
        boolean existsScience = genreRepository.existsByNameIgnoreCase("science fiction")
        Optional<Genre> foundHistorical = genreRepository.findByNameIgnoreCase("historical fiction")

        then:
        existsHistorical
        existsScience
        foundHistorical.isPresent()
        foundHistorical.get().name == "Historical Fiction"
    }

    def "should save and retrieve genre with basic properties"() {
        given:
        Genre genre = createTestGenre("Test Genre")

        when:
        Genre savedGenre = genreRepository.save(genre)
        Genre retrievedGenre = genreRepository.findById(savedGenre.id).orElse(null)

        then:
        retrievedGenre != null
        retrievedGenre.name == "Test Genre"
        retrievedGenre.books.isEmpty()  // No books initially
    }

    def "should handle genre without books"() {
        given:
        Genre genre = createTestGenre("Standalone Genre")

        when:
        Genre savedGenre = genreRepository.save(genre)
        Genre retrievedGenre = genreRepository.findById(savedGenre.id).orElse(null)

        then:
        retrievedGenre != null
        retrievedGenre.books.isEmpty()
    }

    def "should handle genres with different names"() {
        given:
        Genre genre1 = createTestGenre("Drama")
        Genre genre2 = createTestGenre("Comedy")
        genreRepository.save(genre1)
        genreRepository.save(genre2)

        when:
        boolean existsDrama = genreRepository.existsByNameIgnoreCase("drama")
        boolean existsComedy = genreRepository.existsByNameIgnoreCase("comedy")

        then:
        existsDrama
        existsComedy
        genreRepository.count() == 2
    }

    def "should handle empty and null name checks"() {
        given:
        Genre genre = createTestGenre("Valid Genre")
        genreRepository.save(genre)

        when:
        boolean existsEmpty = genreRepository.existsByNameIgnoreCase("")
        boolean existsNull = genreRepository.existsByNameIgnoreCase(null)
        Optional<Genre> foundEmpty = genreRepository.findByNameIgnoreCase("")
        Optional<Genre> foundNull = genreRepository.findByNameIgnoreCase(null)

        then:
        !existsEmpty
        !existsNull
        foundEmpty.isEmpty()
        foundNull.isEmpty()
    }

    def "should handle special characters in genre names"() {
        given:
        Genre genre = createTestGenre("Sci-Fi & Fantasy")

        when:
        Genre savedGenre = genreRepository.save(genre)
        Optional<Genre> foundGenre = genreRepository.findByNameIgnoreCase("sci-fi & fantasy")

        then:
        savedGenre != null
        foundGenre.isPresent()
        foundGenre.get().name == "Sci-Fi & Fantasy"
    }

    def "should handle very long genre names"() {
        given:
        String longName = "A" * 50  // 50 character name (matching DB constraint)
        Genre genre = createTestGenre(longName)

        when:
        Genre savedGenre = genreRepository.save(genre)
        Optional<Genre> foundGenre = genreRepository.findByNameIgnoreCase(longName.toLowerCase())

        then:
        savedGenre != null
        foundGenre.isPresent()
        foundGenre.get().name == longName
    }

    def "should enforce unique constraint on genre names"() {
        given:
        Genre genre1 = createTestGenre("Horror")
        genreRepository.save(genre1)

        when:
        Genre genre2 = createTestGenre("Horror")  // Same name, same case
        genreRepository.save(genre2)

        then:
        // Unique constraint should prevent duplicate genre names
        thrown(Exception)
    }

    def "should handle genres with numbers and symbols"() {
        given:
        Genre genre = createTestGenre("Genre-2024!")

        when:
        Genre savedGenre = genreRepository.save(genre)
        Optional<Genre> foundGenre = genreRepository.findByNameIgnoreCase("genre-2024!")

        then:
        savedGenre != null
        foundGenre.isPresent()
        foundGenre.get().name == "Genre-2024!"
    }

    // Helper methods
    private Genre createTestGenre(String name) {
        return Genre.builder()
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