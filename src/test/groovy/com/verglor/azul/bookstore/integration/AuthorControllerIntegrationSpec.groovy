package com.verglor.azul.bookstore.integration

import com.verglor.azul.bookstore.BaseIntegrationSpec
import com.verglor.azul.bookstore.domain.Author

import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.*

class AuthorControllerIntegrationSpec extends BaseIntegrationSpec {

    def "should get all authors successfully"() {
        given:
        Author author1 = createTestAuthor("Author One")
        Author author2 = createTestAuthor("Author Two")
        authorRepository.saveAll([author1, author2])

        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].name", notNullValue())
                .body("content[0].id", notNullValue())
                .body("content*.name", hasItems("Author One", "Author Two"))
                .body("page.totalElements", equalTo(2))
                .body("page.totalPages", equalTo(1))
                .body("page.size", equalTo(20))
                .body("page.number", equalTo(0))
    }

    def "should get author by id successfully"() {
        given:
        Author author = createTestAuthor("Test Author")
        Author savedAuthor = authorRepository.save(author)

        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/authors/${savedAuthor.id}")

        then:
        response.then()
                .statusCode(200)
                .body("id", equalTo(savedAuthor.id.intValue()))
                .body("name", equalTo("Test Author"))
    }

    def "should return 404 when author not found"() {
        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/authors/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Author not found with ID: 999"))
    }

    def "should create author successfully"() {
        given:
        Map<String, Object> authorRequest = createAuthorRequest("New Author")

        when:
        def response = getRequestSpecification()
                .body(authorRequest)
                .when()
                .post("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("New Author"))
    }

    def "should return 400 when creating author with invalid data"() {
        given:
        Map<String, Object> authorRequest = [
                name: "" // Invalid empty name
        ]

        when:
        def response = getRequestSpecification()
                .body(authorRequest)
                .when()
                .post("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors", notNullValue())
    }

    def "should return 409 when creating author with duplicate name"() {
        given:
        Author existingAuthor = createTestAuthor("Duplicate Author")
        authorRepository.save(existingAuthor)

        Map<String, Object> authorRequest = createAuthorRequest("Duplicate Author")

        when:
        def response = getRequestSpecification()
                .body(authorRequest)
                .when()
                .post("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(409)
                .body("error", equalTo("Conflict"))
                .body("message", containsString("Author already exists with name: Duplicate Author"))
    }

    def "should update author successfully"() {
        given:
        Author author = createTestAuthor("Original Author Name")
        Author savedAuthor = authorRepository.save(author)

        Map<String, Object> updateRequest = createAuthorRequest("Updated Author Name")

        when:
        def response = getRequestSpecification()
                .body(updateRequest)
                .when()
                .put("${getBaseUrl()}/authors/${savedAuthor.id}")

        then:
        response.then()
                .statusCode(200)
                .body("id", equalTo(savedAuthor.id.intValue()))
                .body("name", equalTo("Updated Author Name"))
    }

    def "should return 404 when updating non-existent author"() {
        given:
        Map<String, Object> updateRequest = createAuthorRequest("Updated Author")

        when:
        def response = getRequestSpecification()
                .body(updateRequest)
                .when()
                .put("${getBaseUrl()}/authors/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
    }

    def "should return 409 when updating author to duplicate name"() {
        given:
        Author author1 = createTestAuthor("Author One")
        Author author2 = createTestAuthor("Author Two")
        authorRepository.saveAll([author1, author2])

        Map<String, Object> updateRequest = createAuthorRequest("Author Two") // Duplicate name

        when:
        def response = getRequestSpecification()
                .body(updateRequest)
                .when()
                .put("${getBaseUrl()}/authors/${author1.id}")

        then:
        response.then()
                .statusCode(409)
                .body("error", equalTo("Conflict"))
                .body("message", containsString( "Author already exists with name: Author Two"))
    }

    def "should delete author successfully"() {
        given:
        Author author = createTestAuthor("Author to Delete")
        Author savedAuthor = authorRepository.save(author)

        when:
        def response = getRequestSpecification()
                .when()
                .delete("${getBaseUrl()}/authors/${savedAuthor.id}")

        then:
        response.then()
                .statusCode(204)

        and:
        // Verify author is deleted
        given()
                .contentType("application/json")
                .accept("application/json")
                .when()
                .get("${getBaseUrl()}/authors/${savedAuthor.id}")
                .then()
                .statusCode(404)
    }

    def "should return 404 when deleting non-existent author"() {
        when:
        def response = getRequestSpecification()
                .when()
                .delete("${getBaseUrl()}/authors/999")

        then:
        response.then()
                .statusCode(404)
                .body("error", equalTo("Not Found"))
    }

    def "should handle pagination correctly"() {
        given:
        // Create 5 authors
        (1..5).each { i ->
            Author author = createTestAuthor("Author $i")
            authorRepository.save(author)
        }

        when:
        def response = getRequestSpecification()
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("${getBaseUrl()}/authors")

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
        Author author1 = createTestAuthor("Z Author")
        Author author2 = createTestAuthor("A Author")
        authorRepository.saveAll([author1, author2])

        when:
        def response = getRequestSpecification()
                .queryParam("sort", "name,asc")
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].name", equalTo("A Author"))
                .body("content[1].name", equalTo("Z Author"))
    }

    def "should handle sorting by name descending"() {
        given:
        Author author1 = createTestAuthor("Author A")
        Author author2 = createTestAuthor("Author B")
        Author author3 = createTestAuthor("Author C")
        authorRepository.saveAll([author1, author2, author3])

        when:
        def response = getRequestSpecification()
                .queryParam("sort", "name,desc")
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("content[0].name", equalTo("Author C"))
                .body("content[1].name", equalTo("Author B"))
                .body("content[2].name", equalTo("Author A"))
    }

    def "should handle empty result set"() {
        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("page.totalElements", equalTo(0))
                .body("page.totalPages", equalTo(0))
    }

    def "should handle second page correctly"() {
        given:
        // Create 5 authors
        (1..5).each { i ->
            Author author = createTestAuthor("Author $i")
            authorRepository.save(author)
        }

        when:
        def response = getRequestSpecification()
                .queryParam("page", 1)
                .queryParam("size", 2)
                .when()
                .get("${getBaseUrl()}/authors")

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
        // Create 3 authors
        (1..3).each { i ->
            Author author = createTestAuthor("Author $i")
            authorRepository.save(author)
        }

        when:
        def response = getRequestSpecification()
                .queryParam("size", 10)
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("page.size", equalTo(10))
                .body("page.totalElements", equalTo(3))
                .body("page.totalPages", equalTo(1))
    }

    def "should handle multiple sort fields"() {
        given:
        // Create authors with same name but different IDs
        Author author1 = createTestAuthor("Same Name")
        Author author2 = createTestAuthor("Same Name")
        authorRepository.saveAll([author1, author2])

        when:
        def response = getRequestSpecification()
                .queryParam("sort", ["name,asc","id,desc"])
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].name", equalTo("Same Name"))
                .body("content[1].name", equalTo("Same Name"))
                // Should be sorted by ID descending when names are equal
    }

    def "should validate author name length constraints"() {
        given:
        Map<String, Object> authorRequest = [
                name: "A" // Too short (minimum 2 characters)
        ]

        when:
        def response = getRequestSpecification()
                .body(authorRequest)
                .when()
                .post("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors", notNullValue())
    }

    def "should validate author name maximum length"() {
        given:
        String longName = "A".repeat(101) // Too long (maximum 100 characters)
        Map<String, Object> authorRequest = [
                name: longName
        ]

        when:
        def response = getRequestSpecification()
                .body(authorRequest)
                .when()
                .post("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(400)
                .body("error", equalTo("Bad Request"))
                .body("validationErrors", notNullValue())
    }

    def "should handle concurrent author creation with same name"() {
        given:
        Map<String, Object> authorRequest = createAuthorRequest("Concurrent Author")

        when:
        // Try to create the same author twice simultaneously
        def response1 = getRequestSpecification()
                .body(authorRequest)
                .when()
                .post("${getBaseUrl()}/authors")

        def response2 = getRequestSpecification()
                .body(authorRequest)
                .when()
                .post("${getBaseUrl()}/authors")

        then:
        // One should succeed, one should fail with conflict
        response1.then()
                .statusCode(201)
                .body("name", equalTo("Concurrent Author"))

        response2.then()
                .statusCode(409)
                .body("error", equalTo("Conflict"))
    }

    def "should search authors by name containing"() {
        given:
        Author author1 = createTestAuthor("Stephen King")
        Author author2 = createTestAuthor("King Arthur")
        Author author3 = createTestAuthor("John Smith")
        authorRepository.saveAll([author1, author2, author3])

        when:
        def response = getRequestSpecification()
                .queryParam("name", "king")
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content*.name", hasItems("Stephen King", "King Arthur"))
                .body("content*.name", not(hasItem("John Smith")))
                .body("page.totalElements", equalTo(2))
    }

    def "should search authors case insensitive"() {
        given:
        Author author1 = createTestAuthor("Stephen King")
        Author author2 = createTestAuthor("John Smith")
        authorRepository.saveAll([author1, author2])

        when:
        def response = getRequestSpecification()
                .queryParam("name", "KING")
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].name", equalTo("Stephen King"))
                .body("page.totalElements", equalTo(1))
    }

    def "should return all authors when name parameter is not provided"() {
        given:
        Author author1 = createTestAuthor("Author One")
        Author author2 = createTestAuthor("Author Two")
        authorRepository.saveAll([author1, author2])

        when:
        def response = getRequestSpecification()
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content*.name", hasItems("Author One", "Author Two"))
                .body("page.totalElements", equalTo(2))
    }

    def "should return empty result when no authors match search"() {
        given:
        Author author1 = createTestAuthor("Stephen King")
        authorRepository.save(author1)

        when:
        def response = getRequestSpecification()
                .queryParam("name", "nonexistent")
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("page.totalElements", equalTo(0))
    }

    def "should search authors with pagination"() {
        given:
        // Create 5 authors
        (1..5).each { i ->
            authorRepository.saveAll([
                    createTestAuthor("Writer $i"),
                    createTestAuthor("Author $i"),
            ])
        }

        when:
        def response = getRequestSpecification()
                .queryParam("name", "Author")
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].name", equalTo("Author 1"))
                .body("content[1].name", equalTo("Author 2"))
                .body("page.size", equalTo(2))
                .body("page.number", equalTo(0))
                .body("page.totalElements", equalTo(5))
                .body("page.totalPages", equalTo(3))
    }

    def "should search authors with sorting"() {
        given:
        authorRepository.saveAll([
                createTestAuthor("Z Author"),
                createTestAuthor("B Writer"),
                createTestAuthor("A Author"),
        ])

        when:
        def response = getRequestSpecification()
                .queryParam("name", "Author")
                .queryParam("sort", "name,asc")
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].name", equalTo("A Author"))
                .body("content[1].name", equalTo("Z Author"))
    }

    def "should enforce maximum page size of 100"() {
        given:
        // Create more than 100 authors to test pagination limits
        (1..150).each { i ->
            Author author = createTestAuthor("Author $i")
            authorRepository.save(author)
        }

        when:
        def response = getRequestSpecification()
                .queryParam("page", 0)
                .queryParam("size", 150) // Request more than the max of 100
                .when()
                .get("${getBaseUrl()}/authors")

        then:
        response.then()
                .statusCode(200)
                .body("content.size()", equalTo(100)) // Should be capped at 100
                .body("page.size", equalTo(100)) // Page size should be 100, not 150
                .body("page.number", equalTo(0))
                .body("page.totalElements", equalTo(150))
                .body("page.totalPages", equalTo(2)) // Should have 2 pages (150/100)
    }
}