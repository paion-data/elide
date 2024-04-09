/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.assignedIdLongTests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.elements.Data;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class AssignedIdLongIT extends IntegrationTest {

    @Test
    public void testResponseCodeOnUpdate() {
        Data original = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("assignedIdLong"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("value", 3)
                        )
                )
        );

        Data modified = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("assignedIdLong"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("value", 9)
                        )
                )
        );

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(original)
                .post("/assignedIdLong")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(original.toJSON()));

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(modified)
                .patch("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
