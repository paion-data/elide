/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for UserType.
 */
class GenerateIT extends IntegrationTest {

    @Test
    @Tag("skipInMemory")
    public void testGeneratePost() {

        Resource resource = JsonApiDSL.resource(
                JsonApiDSL.type("generate"),
                JsonApiDSL.id("1")
        );

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(JsonApiDSL.datum(resource))
            .post("/generate")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .body("data.attributes.created", notNullValue());
    }
}
