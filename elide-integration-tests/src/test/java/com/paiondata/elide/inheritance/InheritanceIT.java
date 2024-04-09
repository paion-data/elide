/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.inheritance;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.junit.jupiter.api.Test;

public class InheritanceIT extends IntegrationTest {

    @Test
    public void testEmployeeHierarchy() {

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("manager"),
                                        JsonApiDSL.id(null)
                                )
                        )
                )
                .post("/manager")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"));

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("employee"),
                                        JsonApiDSL.id(null),
                                        JsonApiDSL.attributes(),
                                        JsonApiDSL.relationships(
                                                JsonApiDSL.relation("boss",
                                                        JsonApiDSL.linkage(JsonApiDSL.type("manager"), JsonApiDSL.id("1"))
                                                )
                                        )
                                )
                        )
                )
                .post("/manager/1/minions")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"),
                        "data.relationships.boss.data.id", equalTo("1")
                );

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .when()
                .get("/manager/1")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.id", equalTo("1"),
                        "data.relationships.minions.data.id", contains("1"),
                        "data.relationships.minions.data.type", contains("employee")
                );
    }
}
