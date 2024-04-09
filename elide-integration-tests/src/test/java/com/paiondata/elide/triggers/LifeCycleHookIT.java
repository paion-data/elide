/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.triggers;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.initialization.LifeCycleIntegrationTestApplicationResourceConfig;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.jsonapi.resources.JsonApiEndpoint;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LifeCycleHookIT extends IntegrationTest {

    public LifeCycleHookIT() {
        super(LifeCycleIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @Test
    public void testBillingServiceInvocation() {

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("customerInvoice"),
                                        JsonApiDSL.id("123"),
                                        JsonApiDSL.attributes(
                                                JsonApiDSL.attr("complete", true),
                                                JsonApiDSL.attr("total", 1000)
                                        )
                                )
                        )
                )
                .post("/customerInvoice")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"))
                .body("data.attributes.total", equalTo(1100))
                .body("data.attributes.complete", equalTo(true));
    }
}
