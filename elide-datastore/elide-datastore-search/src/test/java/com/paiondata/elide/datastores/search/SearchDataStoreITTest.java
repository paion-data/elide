/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.search;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.data;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.AbstractApiResourceInitializer;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SearchDataStoreITTest extends AbstractApiResourceInitializer {

    public SearchDataStoreITTest() {
        super(DependencyBinder.class);
    }

    @Test
    public void getEscapedItem() {
        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .when()
            .get("/item?filter[item]=name==*-luc*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", equalTo(Arrays.asList("6")));
    }

    @Test
    public void testObjectIndexing() {
       /* Add a new item */
       given()
           .contentType(JsonApi.MEDIA_TYPE)
           .body(
                   JsonApiDSL.data(
                       JsonApiDSL.resource(
                          JsonApiDSL.type("item"),
                          JsonApiDSL.id(1000),
                          JsonApiDSL.attributes(
                                  JsonApiDSL.attr("name", "Another Drum"),
                                  JsonApiDSL.attr("description", "Onyx Timpani Drum")
                          )
                       )
                   ).toJSON())
           .when()
           .post("/item")
           .then()
           .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        /* This query hits the index */
        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .when()
            .get("/item?filter[item]=name=ini=*DrU*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "3", "1000"));

        /* This query hits the DB */
        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .when()
            .get("/item")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "2", "3", "4", "5", "6", "7", "1000"));

        /* Delete the newly added item */
        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .when()
            .delete("/item/1000")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        /* This query hits the index */
        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .when()
            .get("/item?filter[item]=name==*dru*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "3"));

        /* This query hits the DB */
        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .when()
            .get("/item")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "2", "3", "4", "5", "6", "7"));
    }
}
