/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.tests;

import static com.paiondata.elide.test.graphql.GraphQLDSL.argument;
import static com.paiondata.elide.test.graphql.GraphQLDSL.field;
import static com.paiondata.elide.test.graphql.GraphQLDSL.mutation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.data;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import com.paiondata.elide.initialization.GraphQLIntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.google.common.collect.Sets;
import com.paiondata.elide.test.graphql.GraphQLDSL;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import example.embeddedid.Address;
import example.embeddedid.AddressSerde;
import example.embeddedid.Building;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.Data;

import java.io.IOException;
import java.util.Arrays;

public class EmbeddedIdIT extends GraphQLIntegrationTest {

    protected Address address1 = new Address(0, "Bullion Blvd", 40121);
    protected Address address2 = new Address(1409, "W Green St", 61801);
    protected Address address3 = new Address(1800, "South First Street", 61820);
    protected AddressSerde serde = new AddressSerde();

    @BeforeAll
    public void beforeAll() {
        CoerceUtil.register(Address.class, serde);
    }

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(EntityDictionary.builder().build());
        DataStoreTransaction tx = dataStore.beginTransaction();

        Building building1 = new Building();
        building1.setAddress(address1);
        building1.setName("Fort Knox");

        Building building2 = new Building();
        building2.setAddress(address3);
        building2.setName("Assembly Hall");

        building1.setNeighbors(Sets.newHashSet(building2));
        building2.setNeighbors(Sets.newHashSet(building1));

        tx.createObject(building1, null);
        tx.createObject(building2, null);

        tx.commit(null);
        tx.close();
    }

    @Test
    public void testJsonApiFetchCollection() {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        given()
                .when()
                .get("/building")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        JsonApiDSL.data(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("building"),
                                        JsonApiDSL.id(address1Id),
                                        JsonApiDSL.attributes(
                                                JsonApiDSL.attr("name", "Fort Knox")
                                        ),
                                        JsonApiDSL.relationships(
                                                JsonApiDSL.relation("neighbors",
                                                        JsonApiDSL.linkage(JsonApiDSL.type("building"), JsonApiDSL.id(address3Id))
                                                )
                                        )
                                ),
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("building"),
                                        JsonApiDSL.id(address3Id),
                                        JsonApiDSL.attributes(
                                                JsonApiDSL.attr("name", "Assembly Hall")
                                        ),
                                        JsonApiDSL.relationships(
                                                JsonApiDSL.relation("neighbors",
                                                        JsonApiDSL.linkage(JsonApiDSL.type("building"), JsonApiDSL.id(address1Id))
                                                )
                                        )
                                )
                        ).toJSON())
                );
    }

    @Test
    public void testJsonApiFetchById() {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        given()
                .when()
                .get("/building/" + address1Id)
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                    JsonApiDSL.datum(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("building"),
                                JsonApiDSL.id(address1Id),
                                JsonApiDSL.attributes(
                                        JsonApiDSL.attr("name", "Fort Knox")
                                ),
                                JsonApiDSL.relationships(
                                        JsonApiDSL.relation("neighbors",
                                                JsonApiDSL.linkage(JsonApiDSL.type("building"), JsonApiDSL.id(address3Id))
                                        )
                                )
                        )
                ).toJSON())
        );
    }

    @Test
    public void testJsonApiFetchRelationship() {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        given()
                .when()
                .get("/building/" + address1Id + "/neighbors")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        JsonApiDSL.data(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("building"),
                                        JsonApiDSL.id(address3Id),
                                        JsonApiDSL.attributes(
                                                JsonApiDSL.attr("name", "Assembly Hall")
                                        ),
                                        JsonApiDSL.relationships(
                                                JsonApiDSL.relation("neighbors",
                                                        JsonApiDSL.linkage(JsonApiDSL.type("building"), JsonApiDSL.id(address1Id))
                                                )
                                        )
                                )
                        ).toJSON())
                );
    }

    @Test
    public void testJsonApiFilterById() {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        given()
            .when()
            .get("/building?filter=id=='" + address1Id + "'")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                    JsonApiDSL.data(
                        JsonApiDSL.resource(
                            JsonApiDSL.type("building"),
                            JsonApiDSL.id(address1Id),
                            JsonApiDSL.attributes(
                                JsonApiDSL.attr("name", "Fort Knox")
                            ),
                            JsonApiDSL.relationships(
                                JsonApiDSL.relation("neighbors",
                                JsonApiDSL.linkage(JsonApiDSL.type("building"), JsonApiDSL.id(address3Id))
                            )
                        )
                    )
                ).toJSON()
            ));
    }

    @Test
    public void testJsonApiCreate() {
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(JsonApiDSL.data(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("building"),
                                JsonApiDSL.id(serde.serialize(address2)),
                                JsonApiDSL.attributes(JsonApiDSL.attr(
                                        "name", "Altgeld Hall"
                                ))
                        )

                ))
                .when()
                .post("/building")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(JsonApiDSL.datum(
                        JsonApiDSL.resource(
                            JsonApiDSL.type("building"),
                            JsonApiDSL.id(serde.serialize(address2)),
                            JsonApiDSL.attributes(JsonApiDSL.attr(
                                    "name", "Altgeld Hall"
                            )),
                            JsonApiDSL.relationships(JsonApiDSL.relation("neighbors"))
                        )).toJSON()
                ));
    }


    @Test
    public void testGraphQLFetchCollection() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("address"),
                                        GraphQLDSL.field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("address", serde.serialize(address1)),
                                        GraphQLDSL.field("name", "Fort Knox")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("address", serde.serialize(address3)),
                                        GraphQLDSL.field("name", "Assembly Hall")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLFetchById() throws Exception {
        String addressId = serde.serialize(address1);

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("ids", Arrays.asList(addressId))
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("address"),
                                        GraphQLDSL.field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("address", addressId),
                                        GraphQLDSL.field("name", "Fort Knox")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLFetchRelationship() throws Exception {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("ids", Arrays.asList(address1Id))
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("neighbors",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("name"),
                                                        GraphQLDSL.field("address")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("neighbors",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("name", "Assembly Hall"),
                                                        GraphQLDSL.field("address", address3Id)
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLFilterById() throws Exception {
        String addressId = serde.serialize(address1);

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"id==\\\"" + addressId + "\\\"\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("address"),
                                        GraphQLDSL.field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("address", addressId),
                                        GraphQLDSL.field("name", "Fort Knox")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLCreate() throws Exception {

        @Data
        class SerializedBuilding {
            private String name;
            private String address;
        }

        String addressId = serde.serialize(address2);
        SerializedBuilding building = new SerializedBuilding();
        building.address = addressId;
        building.name = "Altgeld Hall";

        String graphQLRequest = GraphQLDSL.document(
            GraphQLDSL.mutation(
                    GraphQLDSL.selection(
                            GraphQLDSL.field(
                                    "building",
                                    GraphQLDSL.arguments(
                                            GraphQLDSL.argument("op", "UPSERT"),
                                            GraphQLDSL.argument("data", building)
                                    ),
                                    GraphQLDSL.selections(
                                            GraphQLDSL.field("address"),
                                            GraphQLDSL.field("name")
                                    )
                            )
                    )
            )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "building",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("address", addressId),
                                        GraphQLDSL.field("name", "Altgeld Hall")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }
}
