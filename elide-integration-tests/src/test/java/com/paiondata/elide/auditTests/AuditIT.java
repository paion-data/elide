/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.auditTests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.audit.InMemoryLogger;
import com.paiondata.elide.initialization.AuditIntegrationTestApplicationResourceConfig;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.jsonapi.resources.JsonApiEndpoint;
import com.paiondata.elide.test.jsonapi.elements.Resource;
import com.paiondata.elide.test.jsonapi.elements.ResourceLinkage;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for audit functionality.
 */
public class AuditIT extends IntegrationTest {
    private final InMemoryLogger logger = AuditIntegrationTestApplicationResourceConfig.LOGGER;

    public AuditIT() {
        super(AuditIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    private static final Resource AUDIT_1 = JsonApiDSL.resource(
            JsonApiDSL.type("auditEntity"),
            JsonApiDSL.id("1"),
            JsonApiDSL.attributes(
                    JsonApiDSL.attr("value", "test abc")
            )
    );

    private static final Resource AUDIT_1_RELATIONSHIP = JsonApiDSL.resource(
            JsonApiDSL.type("auditEntity"),
            JsonApiDSL.id("1"),
            JsonApiDSL.attributes(
                    JsonApiDSL.attr("value", "updated value")
            ),
            JsonApiDSL.relationships(
                    JsonApiDSL.relation(
                            "otherEntity",
                            true,
                            JsonApiDSL.linkage(
                                    JsonApiDSL.type("auditEntity"),
                                    JsonApiDSL.id("2")
                            )
                    )
            )
    );

    private static final Resource AUDIT_2 = JsonApiDSL.resource(
            JsonApiDSL.type("auditEntity"),
            JsonApiDSL.id("2"),
            JsonApiDSL.attributes(
                    JsonApiDSL.attr("value", "test def")
            ),
            JsonApiDSL.relationships(
                    JsonApiDSL.relation(
                            "otherEntity",
                            true,
                            JsonApiDSL.linkage(
                                    JsonApiDSL.type("auditEntity"),
                                    JsonApiDSL.id("1")
                            )
                    )
            )
    );

    @Test
    public void testAuditOnCreate() {
        String expected = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("auditEntity"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("value", "test abc")
                        ),
                        JsonApiDSL.relationships(
                                JsonApiDSL.relation("otherEntity", (ResourceLinkage[]) null),
                                JsonApiDSL.relation("inverses")
                        )
                )
        ).toJSON();

        // create auditEntity 1 and validate the created entity
        String actual = createAuditEntity(AUDIT_1);

        assertEqualDocuments(actual, expected); // document comparison is needed as the order of relationship can be different
        assertTrue(logger.logMessages.contains("old: null\n"
                + "new: Value: test abc relationship: null"));
        assertTrue(logger.logMessages.contains("Created with value: test abc"));
    }

    @Test
    public void testAuditOnUpdate() {
        String expected = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("auditEntity"),
                        JsonApiDSL.id("2"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("value", "test def")
                        ),
                        JsonApiDSL.relationships(
                                JsonApiDSL.relation(
                                        "otherEntity",
                                        JsonApiDSL.linkage(
                                                JsonApiDSL.type("auditEntity"),
                                                JsonApiDSL.id("1")
                                        )
                                ),
                                JsonApiDSL.relation("inverses")
                        )
                )
        ).toJSON();

        // create auditEntity 1
        createAuditEntity(AUDIT_1);

        // create auditEntity 2 and validate the created entity
        String actual = createAuditEntity(AUDIT_2);

        assertEqualDocuments(actual, expected); // document comparison is needed as the order of relationship can be different

        // update auditEntity 1 directly
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(AUDIT_1_RELATIONSHIP).toJSON()
                )
                .patch("/auditEntity/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        assertTrue(logger.logMessages.contains("Updated relationship (for id: 1): 2"));
        assertTrue(logger.logMessages.contains("Updated value (for id: 1): updated value"));
    }

    @Test
    public void testAuditWithDuplicateLineageEntry() {
        // create auditEntity 1 and 2
        createAuditEntity(AUDIT_1);
        createAuditEntity(AUDIT_2);

        // update auditEntity 1 through the relationship of auditEntity 2
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("auditEntity"),
                                        JsonApiDSL.id("1"),
                                        JsonApiDSL.attributes(
                                                JsonApiDSL.attr("value", "update id 1 through id 2")
                                        )
                                )
                        ).toJSON()
                )
                .patch("/auditEntity/2/otherEntity/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        assertTrue(logger.logMessages.contains("Updated value (for id: 1): update id 1 through id 2"));
    }

    @Test
    public void testAuditUpdateOnInverseCollection() {
        // create auditEntity 1 and 2, update auditEntity 1 to have relationship to auditEntity 2
        createAuditEntity(AUDIT_1);
        createAuditEntity(AUDIT_2);
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(AUDIT_1_RELATIONSHIP).toJSON()
                )
                .patch("/auditEntity/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        assertFalse(logger.logMessages.contains("Inverse entities: [Value: updated value relationship: 2]"));

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("auditEntityInverse"),
                                        JsonApiDSL.id("1"),
                                        JsonApiDSL.relationships(
                                                JsonApiDSL.relation(
                                                        "entities",
                                                        JsonApiDSL.linkage(
                                                                JsonApiDSL.type("auditEntity"),
                                                                JsonApiDSL.id("1")
                                                        )
                                                )
                                        )
                                )
                        ).toJSON()
                )
                .post("/auditEntityInverse")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        assertTrue(logger.logMessages.contains("Entity with id 1 now has inverse list [AuditEntityInverse{id=1, entities=[Value: updated value relationship: 2]}]"));
        assertTrue(logger.logMessages.contains("Inverse entities: [Value: updated value relationship: 2]"));

        // This message may have been added on create. Remove it so we don't get a false positive.
        // NOTE: Our internal audit loggers handle this behavior by ignoring update messages associated with
        //       creations, but this is the default behavior to provide flexibility for any use case.
        logger.logMessages.remove("Entity with id 1 now has inverse list []");
        logger.logMessages.remove("Inverse entities: []");

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body("{\"data\":[]}")
                .patch("/auditEntity/1/relationships/inverses")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        assertTrue(logger.logMessages.contains("Entity with id 1 now has inverse list []"));
        assertTrue(logger.logMessages.contains("Inverse entities: []"));
    }

    private String createAuditEntity(Resource auditEntity) {
        return given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(auditEntity).toJSON()
                )
                .post("/auditEntity")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .asString();
    }
}
