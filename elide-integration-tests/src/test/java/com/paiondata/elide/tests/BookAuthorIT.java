/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BookAuthorIT extends IntegrationTest {

    private static final String ATTRIBUTES = "attributes";
    private static final String RELATIONSHIPS = "relationships";
    private static final String INCLUDED = "included";

    private static final Resource HEMINGWAY = JsonApiDSL.resource(
            JsonApiDSL.type("author"),
            JsonApiDSL.attributes(
                    JsonApiDSL.attr("name", "Ernest Hemingway")
            )
    );

    private static final Resource THE_OLD_MAN_AND_THE_SEA = JsonApiDSL.resource(
            JsonApiDSL.type("book"),
            JsonApiDSL.attributes(
                    JsonApiDSL.attr("title", "The Old Man and the Sea"),
                    JsonApiDSL.attr("genre", "Literary Fiction"),
                    JsonApiDSL.attr("language", "English")
            )
    );

    private static final Resource HEMINGWAY_RELATIONSHIP = JsonApiDSL.resource(
            JsonApiDSL.type("author"),
            JsonApiDSL.id(1)
    );

    private static final Resource ORSON_SCOTT_CARD = JsonApiDSL.resource(
            JsonApiDSL.type("author"),
            JsonApiDSL.attributes(
                    JsonApiDSL.attr("name", "Orson Scott Card")
            )
    );

    private static final Resource ENDERS_GAME = JsonApiDSL.resource(
            JsonApiDSL.type("book"),
            JsonApiDSL.attributes(
                    JsonApiDSL.attr("title", "Ender's Game"),
                    JsonApiDSL.attr("genre", "Science Fiction"),
                    JsonApiDSL.attr("language", "English")
            )
    );

    private static final Resource ORSON_RELATIONSHIP = JsonApiDSL.resource(
            JsonApiDSL.type("author"),
            JsonApiDSL.id(2)
    );

    private static final Resource FOR_WHOM_THE_BELL_TOLLS = JsonApiDSL.resource(
            JsonApiDSL.type("book"),
            JsonApiDSL.attributes(
                    JsonApiDSL.attr("title", "For Whom the Bell Tolls"),
                    JsonApiDSL.attr("genre", "Literary Fiction"),
                    JsonApiDSL.attr("language", "English")
            )
    );

    @BeforeEach
    public void setup() {
        dataStore.populateEntityDictionary(EntityDictionary.builder().build());

        // Create Author: Ernest Hemingway
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(HEMINGWAY).toJSON()
                )
                .post("/author")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Book: The Old Man and the Sea
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(THE_OLD_MAN_AND_THE_SEA).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Ernest Hemingway -> The Old Man and the Sea
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(HEMINGWAY_RELATIONSHIP).toJSON()
                )
                .patch("/book/1/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Create Author: Orson Scott Card
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(ORSON_SCOTT_CARD).toJSON()
                )
                .post("/author")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Book: Ender's Game
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(ENDERS_GAME).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Orson Scott Card -> Ender's Game
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(ORSON_RELATIONSHIP).toJSON()
                )
                .patch("/book/2/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Create Book: For Whom the Bell Tolls
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(FOR_WHOM_THE_BELL_TOLLS).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Ernest Hemingway -> For Whom the Bell Tolls
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(HEMINGWAY_RELATIONSHIP).toJSON()
                )
                .patch("/book/3/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSparseSingleDataFieldValue() throws Exception {
        JsonNode responseBody = mapper.readTree(
                given()
                        .contentType(JsonApi.MEDIA_TYPE)
                        .accept(JsonApi.MEDIA_TYPE)
                        .param("include", "authors")
                        .param("fields[book]", "title")
                        .get("/book")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            assertTrue(bookNode.has(ATTRIBUTES));
            assertFalse(bookNode.has(RELATIONSHIPS));

            JsonNode attributes = bookNode.get(ATTRIBUTES);
            assertEquals(1, attributes.size());
            assertTrue(attributes.has("title"));
        }

        assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            assertFalse(include.has(ATTRIBUTES));
            assertFalse(include.has(RELATIONSHIPS));
        }
    }

    @Test
    public void testSparseTwoDataFieldValuesNoIncludes() throws Exception {
        JsonNode responseBody = mapper.readTree(
                given()
                        .contentType(JsonApi.MEDIA_TYPE)
                        .accept(JsonApi.MEDIA_TYPE)
                        .param("fields[book]", "title,language")
                        .get("/book")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            assertTrue(bookNode.has(ATTRIBUTES));
            assertFalse(bookNode.has(RELATIONSHIPS));

            JsonNode attributes = bookNode.get(ATTRIBUTES);
            assertEquals(2, attributes.size());
            assertTrue(attributes.has("title"));
            assertTrue(attributes.has("language"));
        }

        assertFalse(responseBody.has(INCLUDED));
    }

    @Test
    public void testSparseNoFilters() throws Exception {
        JsonNode responseBody = mapper.readTree(
                given()
                        .contentType(JsonApi.MEDIA_TYPE)
                        .accept(JsonApi.MEDIA_TYPE)
                        .param("include", "authors")
                        .get("/book")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            assertTrue(bookNode.has(ATTRIBUTES));
            JsonNode attributes = bookNode.get(ATTRIBUTES);
            assertTrue(attributes.has("title"));
            assertTrue(attributes.has("language"));
            assertTrue(attributes.has("genre"));

            assertTrue(bookNode.has(RELATIONSHIPS));
            JsonNode relationships = bookNode.get(RELATIONSHIPS);
            assertTrue(relationships.has("authors"));
        }

        assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            assertTrue(include.has(ATTRIBUTES));
            JsonNode attributes = include.get(ATTRIBUTES);
            assertTrue(attributes.has("name"));

            assertTrue(include.has(RELATIONSHIPS));
            JsonNode relationships = include.get(RELATIONSHIPS);
            assertTrue(relationships.has("books"));
        }
    }

    @Test
    public void testTwoSparseFieldFilters() throws Exception {
        JsonNode responseBody = mapper.readTree(
                given()
                        .contentType(JsonApi.MEDIA_TYPE)
                        .accept(JsonApi.MEDIA_TYPE)
                        .param("include", "authors")
                        .param("fields[book]", "title,genre,authors")
                        .param("fields[author]", "name")
                        .get("/book")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            assertTrue(bookNode.has(ATTRIBUTES));
            JsonNode attributes = bookNode.get(ATTRIBUTES);
            assertEquals(2, attributes.size());
            assertTrue(attributes.has("title"));
            assertTrue(attributes.has("genre"));

            assertTrue(bookNode.has(RELATIONSHIPS));
            JsonNode relationships = bookNode.get(RELATIONSHIPS);
            assertTrue(relationships.has("authors"));
        }

        assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            assertTrue(include.has(ATTRIBUTES));
            JsonNode attributes = include.get(ATTRIBUTES);
            assertTrue(attributes.has("name"));

            assertFalse(include.has(RELATIONSHIPS));
        }
    }
}
