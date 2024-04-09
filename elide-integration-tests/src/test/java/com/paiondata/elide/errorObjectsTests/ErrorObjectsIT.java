/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.errorObjectsTests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;

import java.io.IOException;

public class ErrorObjectsIT extends IntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testJsonAPIErrorObjects() throws IOException {
        JsonNode errors = objectMapper.readTree(
            given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                    JsonApiDSL.datum(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("nocreate"),
                                JsonApiDSL.id("1")
                        )
                    )
                )
                .post("/nocreate")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .extract().body().asString());

        for (JsonNode errorNode : errors.get("errors")) {
            assertTrue(errorNode.isObject(), "expected error should be object");
            assertTrue(errorNode.has("detail"), "JsonAPI error should have 'detail'");
        }
    }

    @Test
    public void testGraphQLErrorObjects() throws IOException {
        // this is an incorrectly formatted query, which should result in a 400 error being thrown
        String request = "mutation { nocreate(op: UPSERT, data:{id:\"1\"}) { edges { node { id } } } }";

        JsonNode errors = objectMapper.readTree(
            given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().body().asString());

        for (JsonNode errorNode : errors.get("errors")) {
            assertTrue(errorNode.isObject(), "expected error should be object");
            assertTrue(errorNode.has("message"), "GraphQL error should have 'message'");
        }
    }
}
