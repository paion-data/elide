/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.google.common.collect.ImmutableSet;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;
import com.paiondata.elide.test.jsonapi.elements.Relation;

import example.Embedded;
import example.Left;
import example.Right;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Integration test for embedded collections.
 */
public class EmbeddedIT extends IntegrationTest {

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(EntityDictionary.builder().build());
        DataStoreTransaction tx = dataStore.beginTransaction();
        Embedded embedded = new Embedded(); // id 1
        embedded.setSegmentIds(ImmutableSet.of(3L, 4L, 5L));

        tx.createObject(embedded, null);

        Left left = new Left();
        Right right = new Right();

        left.setOne2one(right);
        right.setOne2one(left);

        tx.createObject(left, null);
        tx.createObject(right, null);

        tx.commit(null);
        tx.close();
    }

    @Test
    void testEmbedded() {
        Resource resource = JsonApiDSL.resource(
                JsonApiDSL.type("embedded"),
                JsonApiDSL.id("1"),
                JsonApiDSL.attributes(
                        JsonApiDSL.attr("segmentIds", new int[]{3, 4, 5})
                )
        );

        given().when().get("/embedded/1").then().statusCode(HttpStatus.SC_OK).body(equalTo(JsonApiDSL.datum(resource).toJSON()));
    }

    @Test
    void testOne2One() throws Exception {
        Resource resource = JsonApiDSL.resource(
                JsonApiDSL.type("right"),
                JsonApiDSL.id("1"),
                JsonApiDSL.relationships(
                        JsonApiDSL.relation("noUpdate"),
                        JsonApiDSL.relation("many2one", Relation.TO_ONE),
                        JsonApiDSL.relation("noUpdateOne2One", Relation.TO_ONE),
                        JsonApiDSL.relation("one2one", Relation.TO_ONE,
                                JsonApiDSL.linkage(JsonApiDSL.type("left"), JsonApiDSL.id("1"))
                        ),
                        JsonApiDSL.relation("noDelete")
                )
        );

        given()
                .when()
                .get("/right/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(JsonApiDSL.datum(resource), true));
    }
}
