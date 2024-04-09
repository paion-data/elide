/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for UserType.
 */
class UserTypeIT extends IntegrationTest {

    @Data
    @AllArgsConstructor
    private class Address {
        private String street;
        private String state;
        private Zip zip;

    }

    @Data
    @AllArgsConstructor
    private class Zip {
        private String zip;
        private String plusFour;
    }

    @Test
    @Tag("skipInMemory")
    public void testUserTypePost() throws Exception {
        Resource resource = JsonApiDSL.resource(
                JsonApiDSL.type("person"),
                JsonApiDSL.id("1"),
                JsonApiDSL.attributes(
                        JsonApiDSL.attr("name", "AK"),
                        JsonApiDSL.attr("address", new Address(
                                "123 AnyStreet Dr",
                                "IL",
                                new Zip("61820", "1234")
                        )),
                        JsonApiDSL.attr("alternateAddress", new Address(
                                "XYZ AnyStreet Dr",
                                "IL",
                                new Zip("61820", "1234")
                        ))
                )
        );

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(JsonApiDSL.datum(resource))
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/person/1")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(jsonEquals(JsonApiDSL.datum(resource), true));
    }

    @Test
    @Tag("skipInMemory")
    public void testUserTypePatch() throws Exception {
        Resource original = JsonApiDSL.resource(
                JsonApiDSL.type("person"),
                JsonApiDSL.id("2"),
                JsonApiDSL.attributes(
                        JsonApiDSL.attr("name", "JK"),
                        JsonApiDSL.attr("address", new Address(
                                "456 AnyStreet Dr",
                                "IL",
                                new Zip("61822", "567")
                        )),
                        JsonApiDSL.attr("alternateAddress", new Address(
                                "XYZ AnyStreet Dr",
                                "IL",
                                new Zip("61820", "1234")
                        ))
                )
        );

        Resource modified = JsonApiDSL.resource(
                JsonApiDSL.type("person"),
                JsonApiDSL.id("2"),
                JsonApiDSL.attributes(
                        JsonApiDSL.attr("name", "DC"),
                        JsonApiDSL.attr("address", new Address(
                                "456 AnyRoad Ave",
                                "AZ",
                                new Zip("85001", "9999")
                        )),
                        JsonApiDSL.attr("alternateAddress", new Address(
                                "ABC AnyStreet Dr",
                                "CO",
                                new Zip("12345", "1234")
                        ))
                )
        );

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(JsonApiDSL.datum(original))
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(JsonApiDSL.datum(modified))
            .patch("/person/2")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/person/2")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(jsonEquals(JsonApiDSL.datum(modified), true));
    }

    @Test
    @Tag("skipInMemory")
    public void testUserTypeMissingUserTypeField() throws Exception {
        Resource resource = JsonApiDSL.resource(
                JsonApiDSL.type("person"),
                JsonApiDSL.id("3"),
                JsonApiDSL.attributes(
                        JsonApiDSL.attr("name", "DM")
                )
        );

        Resource expected = JsonApiDSL.resource(
                JsonApiDSL.type("person"),
                JsonApiDSL.id("3"),
                JsonApiDSL.attributes(
                        JsonApiDSL.attr("name", "DM"),
                        JsonApiDSL.attr("address", null),
                        JsonApiDSL.attr("alternateAddress", null)
                )
        );


        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(JsonApiDSL.datum(resource))
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/person/3")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(jsonEquals(JsonApiDSL.datum(expected), true));
    }

    @Test
    @Tag("skipInMemory")
    public void testUserTypeMissingUserTypeProperties() throws Exception {

        Map<String, Object> partialZip = new HashMap<>();
        partialZip.put("zip", "60412");

        Map<String, Object> partialAddress = new HashMap<>();
        partialAddress.put("street", "1400 AnyAve St");
        partialAddress.put("zip", partialZip);

        Resource resource = JsonApiDSL.resource(
            JsonApiDSL.type("person"),
            JsonApiDSL.id("4"),
            JsonApiDSL.attributes(
                JsonApiDSL.attr("name", "WC"),
                JsonApiDSL.attr("address", partialAddress),
                JsonApiDSL.attr("alternateAddress", partialAddress)
            )
        );

        Resource expected = JsonApiDSL.resource(
            JsonApiDSL.type("person"),
            JsonApiDSL.id("4"),
            JsonApiDSL.attributes(
                JsonApiDSL.attr("name", "WC"),
                JsonApiDSL.attr("address", new Address(
                    "1400 AnyAve St",
                    null,
                    new Zip("60412", null)
                )),
                JsonApiDSL.attr("alternateAddress", new Address(
                    "1400 AnyAve St",
                    null,
                    new Zip("60412", null)
                ))
            )
        );

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .body(JsonApiDSL.datum(resource))
            .post("/person")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .contentType(JsonApi.MEDIA_TYPE)
            .accept(JsonApi.MEDIA_TYPE)
            .get("/person/4")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(jsonEquals(JsonApiDSL.datum(expected), true));
    }
}
