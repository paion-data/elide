/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.errorEncodingTests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.utils.JsonParser;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class EncodedErrorObjectsIT extends IntegrationTest {

    private static final String GRAPHQL_CONTENT_TYPE = "application/json";
    private final JsonParser jsonParser = new JsonParser();

    @Test
    public void invalidAttributeException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/InvalidAttributeException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidAttributeException.json");
        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(request)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test
    public void invalidCollectionException() {
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidCollectionErrorObject.json");
        given()
            .get("/unknown")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .body(equalTo(expected));
    }

    @Test
    public void invalidEntityBodyException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyExceptionErrorObject.json");
        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(request)
            .patch("/parent")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test
    public void invalidObjectIdentifierException() {
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidObjectIdentifierExceptionErrorObject.json");
        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/parent/100")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .body(equalTo(expected));
    }

    @Test
    public void invalidValueException() {
        Resource requestBody = JsonApiDSL.resource(
                JsonApiDSL.type("invoice"),
                JsonApiDSL.id("a")
        );

        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidValueExceptionErrorObject.json");
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(JsonApiDSL.datum(requestBody))
                .post("/invoice")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void jsonPatchExtensionException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionExceptionErrorObject.json");
        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(request).patch()
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test
    public void transactionException() {
        // intentionally forget the comma between type and id to force a transaction exception
        String request = "{\"data\": {\"type\": \"invoice\" \"id\": 100}}";
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/transactionExceptionErrorObject.json");
        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(request).post("/invoice")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(equalTo(expected));
    }

    @Test
    public void graphQLMutationError() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLMutationError.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLMutationError.json");
        given()
            .contentType(GRAPHQL_CONTENT_TYPE)
            .accept(GRAPHQL_CONTENT_TYPE)
            .body(request)
            .post("/graphQL")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }

    @Test
    public void graphQLFetchError() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLFetchError.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLFetchErrorObjectEncoded.json");
        given()
            .contentType(GRAPHQL_CONTENT_TYPE)
            .accept(GRAPHQL_CONTENT_TYPE)
            .body(request)
            .post("/graphQL")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(equalTo(expected));
    }
}
