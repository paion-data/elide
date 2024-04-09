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
import com.paiondata.elide.initialization.VerboseEncodedErrorResponsesTestApplicationResourceConfig;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.jsonapi.resources.JsonApiEndpoint;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Test class for checking encoding on verbose error messages.
 * Note that many exceptions don't really differ very much/at all between verbose and non-verbose, so many
 * of these tests are similar to {@link EncodedErrorResponsesIT}.
 */
public class VerboseEncodedErrorResponsesIT extends IntegrationTest {

    private static final String GRAPHQL_CONTENT_TYPE = "application/json";
    private final JsonParser jsonParser = new JsonParser();

    public VerboseEncodedErrorResponsesIT() {
        super(VerboseEncodedErrorResponsesTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

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
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidCollection.json");
        given()
            .get("/unknown")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .body(equalTo(expected));
    }

    @Test
    public void invalidEntityBodyException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyException.json");
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
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidObjectIdentifierException.json");
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

        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidValueExceptionVerbose.json");
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
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionException.json");
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
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/transactionException.json");
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
}
