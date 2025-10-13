package com.verglor.azul.bookstore.controller

import com.verglor.azul.bookstore.BaseIntegrationSpec
import com.verglor.azul.bookstore.domain.Genre
import org.springframework.transaction.annotation.Transactional

import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.*

class GenreControllerIntegrationSpec extends BaseIntegrationSpec {

    def "should get all genres successfully"() {
        given:
        Genre genre1 = createTestGenre("Fiction")
        Genre genre2 = createTestGenre("Science")
        genreRepository.saveAll([genre1, genre2])

        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].name", notNullValue())
                .body("content[0].id", notNullValue())
                .body("content*.name", hasItems("Fiction", "Science"))
                .body("page.totalElements", equalTo(2))
                .body("page.totalPages", equalTo(1))
                .body("page.size", equalTo(20))
                .body("page.number", equalTo(0))
    }

    def "should get genre by id successfully"() {
        given:
        Genre genre = createTestGenre("Test Genre")
        Genre savedGenre = genreRepository.save(genre)

        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/genres/${savedGenre.id}")

        then:
        response.then()
                .statusCode(200)
                .body("id", equalTo(savedGenre.id.intValue()))
                .body("name", equalTo("Test Genre"))
    }

    def "should return 404 when genre not found"() {
        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/genres/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Genre not found with ID: 999"))
    }

    def "should create genre successfully"() {
        given:
        Map<String, Object> genreRequest = createGenreRequest("New Genre")

        when:
        def response = getRequestSpecification()
                .body(genreRequest)
                .when()
                .post("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("New Genre"))
    }

    def "should return 400 when creating genre with invalid data"() {
        given:
        Map<String, Object> genreRequest = [
                name: "" // Invalid empty name
        ]

        when:
        def response = getRequestSpecification()
                .body(genreRequest)
                .when()
                .post("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors", notNullValue())
    }

    def "should return 409 when creating genre with duplicate name"() {
        given:
        Genre existingGenre = createTestGenre("Duplicate Genre")
        genreRepository.save(existingGenre)

        Map<String, Object> genreRequest = createGenreRequest("Duplicate Genre")

        when:
        def response = getRequestSpecification()
                .body(genreRequest)
                .when()
                .post("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(409)
                .body("error", equalTo("Conflict"))
                .body("message", containsString("Genre already exists with name: Duplicate Genre"))
    }

    def "should update genre successfully"() {
        given:
        Genre genre = createTestGenre("Original Genre Name")
        Genre savedGenre = genreRepository.save(genre)

        Map<String, Object> updateRequest = createGenreRequest("Updated Genre Name")

        when:
        def response = getRequestSpecification()
                .body(updateRequest)
                .when()
                .put("${getBaseUrl()}/genres/${savedGenre.id}")

        then:
        response.then()
                .statusCode(200)
                .body("id", equalTo(savedGenre.id.intValue()))
                .body("name", equalTo("Updated Genre Name"))
    }

    def "should return 404 when updating non-existent genre"() {
        given:
        Map<String, Object> updateRequest = createGenreRequest("Updated Genre")

        when:
        def response = getRequestSpecification()
                .body(updateRequest)
                .when()
                .put("${getBaseUrl()}/genres/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
    }

    def "should return 409 when updating genre to duplicate name"() {
        given:
        Genre genre1 = createTestGenre("Genre One")
        Genre genre2 = createTestGenre("Genre Two")
        genreRepository.saveAll([genre1, genre2])

        Map<String, Object> updateRequest = createGenreRequest("Genre Two") // Duplicate name

        when:
        def response = getRequestSpecification()
                .body(updateRequest)
                .when()
                .put("${getBaseUrl()}/genres/${genre1.id}")

        then:
        response.then()
                .statusCode(409)
                .body("error", equalTo("Conflict"))
                .body("message", containsString("Another genre already exists with name: Genre Two"))
    }

    def "should delete genre successfully"() {
        given:
        Genre genre = createTestGenre("Genre to Delete")
        Genre savedGenre = genreRepository.save(genre)

        when:
        def response = getRequestSpecification()
                .when()
                .delete("${getBaseUrl()}/genres/${savedGenre.id}")

        then:
        response.then()
                .statusCode(204)

        and:
        // Verify genre is deleted
        given()
                .contentType("application/json")
                .accept("application/json")
                .when()
                .get("${getBaseUrl()}/genres/${savedGenre.id}")
                .then()
                .statusCode(404)
    }

    def "should return 404 when deleting non-existent genre"() {
        when:
        def response = getRequestSpecification()
                .when()
                .delete("${getBaseUrl()}/genres/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
    }

    def "should handle pagination correctly"() {
        given:
        // Create 5 genres
        (1..5).each { i ->
            Genre genre = createTestGenre("Genre $i")
            genreRepository.save(genre)
        }

        when:
        def response = getRequestSpecification()
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("page.size", equalTo(2))
                .body("page.number", equalTo(0))
                .body("page.totalElements", equalTo(5))
                .body("page.totalPages", equalTo(3))
    }

    def "should handle sorting correctly"() {
        given:
        Genre genre1 = createTestGenre("Z Genre")
        Genre genre2 = createTestGenre("A Genre")
        genreRepository.saveAll([genre1, genre2])

        when:
        def response = getRequestSpecification()
                .queryParam("sort", "name,asc")
                .when()
                .get("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].name", equalTo("A Genre"))
                .body("content[1].name", equalTo("Z Genre"))
    }

    def "should handle sorting by name descending"() {
        given:
        Genre genre1 = createTestGenre("Genre A")
        Genre genre2 = createTestGenre("Genre B")
        Genre genre3 = createTestGenre("Genre C")
        genreRepository.saveAll([genre1, genre2, genre3])

        when:
        def response = getRequestSpecification()
                .queryParam("sort", "name,desc")
                .when()
                .get("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("content[0].name", equalTo("Genre C"))
                .body("content[1].name", equalTo("Genre B"))
                .body("content[2].name", equalTo("Genre A"))
    }

    def "should handle empty result set"() {
        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("page.totalElements", equalTo(0))
                .body("page.totalPages", equalTo(0))
    }

    def "should handle second page correctly"() {
        given:
        // Create 5 genres
        (1..5).each { i ->
            Genre genre = createTestGenre("Genre $i")
            genreRepository.save(genre)
        }

        when:
        def response = getRequestSpecification()
                .queryParam("page", 1)
                .queryParam("size", 2)
                .when()
                .get("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("page.size", equalTo(2))
                .body("page.number", equalTo(1))
                .body("page.totalElements", equalTo(5))
                .body("page.totalPages", equalTo(3))
    }

    def "should handle large page size"() {
        given:
        // Create 3 genres
        (1..3).each { i ->
            Genre genre = createTestGenre("Genre $i")
            genreRepository.save(genre)
        }

        when:
        def response = getRequestSpecification()
                .queryParam("size", 10)
                .when()
                .get("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("page.size", equalTo(10))
                .body("page.totalElements", equalTo(3))
                .body("page.totalPages", equalTo(1))
    }

    def "should validate genre name length constraints"() {
        given:
        Map<String, Object> genreRequest = [
                name: "A" // Too short (minimum 2 characters)
        ]

        when:
        def response = getRequestSpecification()
                .body(genreRequest)
                .when()
                .post("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors", notNullValue())
    }

    def "should validate genre name maximum length"() {
        given:
        String longName = "A".repeat(51) // Too long (maximum 50 characters)
        Map<String, Object> genreRequest = [
                name: longName
        ]

        when:
        def response = getRequestSpecification()
                .body(genreRequest)
                .when()
                .post("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors", notNullValue())
    }

    def "should handle edge case with special characters in genre name"() {
        given:
        Map<String, Object> genreRequest = [
                name: "Sci-Fi & Fantasy"
        ]

        when:
        def response = getRequestSpecification()
                .body(genreRequest)
                .when()
                .post("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(201)
                .body("name", equalTo("Sci-Fi & Fantasy"))
    }

    def "should handle genre name with numbers"() {
        given:
        Map<String, Object> genreRequest = [
                name: "Genre 2024"
        ]

        when:
        def response = getRequestSpecification()
                .body(genreRequest)
                .when()
                .post("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(201)
                .body("name", equalTo("Genre 2024"))
    }

    def "should handle genre name with unicode characters"() {
        given:
        Map<String, Object> genreRequest = [
                name: "文学 (Literature)"
        ]

        when:
        def response = getRequestSpecification()
                .body(genreRequest)
                .when()
                .post("${getBaseUrl()}/genres")

        then:
        response.then()
                .statusCode(201)
                .body("name", equalTo("文学 (Literature)"))
    }
}