/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.extension.test;

//import static com.paiondata.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.paiondata.elide.test.graphql.GraphQLDSL.field;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.data;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.paiondata.elide.Elide;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.extension.test.models.Book;
import com.paiondata.elide.extension.test.models.DenyCheck;
import com.paiondata.elide.extension.test.models.Supplier;
import com.paiondata.elide.test.graphql.GraphQLDSL;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.apache.http.HttpStatus;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

public class ElideExtensionTest {

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest UNIT_TEST = new QuarkusUnitTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                .addAsResource("application.properties")
                .addClass(Book.class)
                .addClass(Supplier.class)
                .addClass(DenyCheck.class));

    @Inject
    EntityDictionary dictionary;

    @Inject
    Elide elide;

    @Inject
    Injector injector;

    @Test
    public void testBookJsonApiEndpoint() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        JsonApiDSL.data(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("book"),
                                        JsonApiDSL.id(1),
                                        JsonApiDSL.attributes(
                                                JsonApiDSL.attr("title", "foo")
                                        )
                                )
                        )
                )
                .post("/book")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_CREATED);
        RestAssured.when().get("/test-jsonapi/book").then().log().all().statusCode(200);
    }

    @Test
    public void testBookGraphqlEndpoint() {
        String query = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
                                )
                        )
                )
        ).toQuery();

        String wrapped = String.format("{ \"query\" : \"%s\" }", query);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(wrapped)
                .post("/test-graphql")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void shouldDisallowSupplierCreation() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        JsonApiDSL.data(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("supplier"),
                                        JsonApiDSL.id(1),
                                        JsonApiDSL.attributes(
                                                JsonApiDSL.attr("name", "foo")
                                        )
                                )
                        )
                )
                .post("/supplier")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testSwaggerCollectionEndpoint() {
        RestAssured.when().get("/test-apiDocs").then().log().all().statusCode(200);
    }

    @Test
    public void testSwaggerApiEndpoint() {
        RestAssured.when().get("/test-apiDocs/api").then().log().all().statusCode(200);
    }

    @Test
    public void testInjection() {
        EntityDictionary dictionary = injector.instantiate(EntityDictionary.class);
        assertNotNull(dictionary);

        Book test = injector.instantiate(Book.class);
        assertNotNull(test);
    }
}
