/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.fieldLevelTest;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class FieldLevelParentClassIdIT extends IntegrationTest {
    @Test
    public void testResponseCodeOnUpdate() {
        Resource original = JsonApiDSL.resource(
                JsonApiDSL.type("fieldLevelChild"),
                JsonApiDSL.id("1"),
                JsonApiDSL.attributes(
                        JsonApiDSL.attr("childField", "someValue"),
                        JsonApiDSL.attr("parentField", "parentValue")
                )
        );

        Resource modified = JsonApiDSL.resource(
                JsonApiDSL.type("fieldLevelChild"),
                JsonApiDSL.id("1"),
                JsonApiDSL.attributes(
                        JsonApiDSL.attr("childField", "someOtherValue"),
                        JsonApiDSL.attr("parentField", "aNewParentValue")
                )
        );

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(JsonApiDSL.datum(original))
                .post("/fieldLevelChild")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(JsonApiDSL.datum(original).toJSON()));

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(JsonApiDSL.datum(modified))
                .patch("/fieldLevelChild/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
