/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.integration;

import static com.paiondata.elide.test.graphql.GraphQLDSL.argument;
import static com.paiondata.elide.test.graphql.GraphQLDSL.field;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.data;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.paiondata.elide.core.datastore.test.DataStoreTestHarness;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.datastores.aggregation.AggregationDataStore;
import com.paiondata.elide.datastores.aggregation.framework.RedisAggregationDataStoreTestHarness;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.test.graphql.GraphQLDSL;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.persistence.EntityManagerFactory;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Integration tests for {@link AggregationDataStore} using Redis for cache.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisAggregationDataStoreIntegrationTest extends AggregationDataStoreIntegrationTest {
    private static final int PORT = 6379;

    private RedisServer redisServer;

    public RedisAggregationDataStoreIntegrationTest() {
        super();
    }

    @BeforeAll
    public void beforeAll() {
        super.beforeAll();
        try {
            redisServer = new RedisServer(PORT);
            redisServer.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    public void afterEverything() {
        try {
            redisServer.stop();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected DataStoreTestHarness createHarness() {

        ConnectionDetails defaultConnectionDetails = createDefaultConnectionDetails();

        EntityManagerFactory emf = createEntityManagerFactory();

        Map<String, ConnectionDetails> connectionDetailsMap = createConnectionDetailsMap(defaultConnectionDetails);

        return new RedisAggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, VALIDATOR);
    }

    @Test
    public void parameterizedJsonApiColumnTest() throws Exception {
        when()
            .get("/SalesNamespace_orderDetails?filter=deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'&fields[SalesNamespace_orderDetails]=orderRatio")
            .then()
            .body(equalTo(
                JsonApiDSL.data(
                    JsonApiDSL.resource(
                        JsonApiDSL.type("SalesNamespace_orderDetails"),
                        JsonApiDSL.id("0"),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("orderRatio", new BigDecimal("1.0000000000000000000000000000000000000000"))
                        )
                    )
                ).toJSON())
            )
            .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void parameterizedGraphQLFilterNoAliasTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"orderRatio[numerator:orderMax][denominator:orderMax]>=.5;deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderRatio", "ratio1", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("numerator", "\"orderMax\""),
                                                GraphQLDSL.argument("denominator", "\"orderMax\"")
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("ratio1", 1.0)
                                )
                        )
                )
        ).toResponse();


        runQueryWithExpectedResult(graphQLRequest, expected);

        // Call the Query Again to hit the cache to retrieve the results
        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void parameterizedGraphQLFilterWithAliasTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"ratio1>=.5;deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderRatio", "ratio1", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("numerator", "\"orderMax\""),
                                                GraphQLDSL.argument("denominator", "\"orderMax\"")
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("ratio1", 1.0)
                                )
                        )
                )
        ).toResponse();


        runQueryWithExpectedResult(graphQLRequest, expected);

        // Call the Query Again to hit the cache to retrieve the results
        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    // Use Non Dynamic Model for caching
    @Test
    public void basicAggregationTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"highScore\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore"),
                                        GraphQLDSL.field("overallRating"),
                                        GraphQLDSL.field("countryIsoCode"),
                                        GraphQLDSL.field("playerRank")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 1000),
                                        GraphQLDSL.field("overallRating", "Good"),
                                        GraphQLDSL.field("countryIsoCode", "HKG"),
                                        GraphQLDSL.field("playerRank", 3)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 1234),
                                        GraphQLDSL.field("overallRating", "Good"),
                                        GraphQLDSL.field("countryIsoCode", "USA"),
                                        GraphQLDSL.field("playerRank", 1)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 3147483647L),
                                        GraphQLDSL.field("overallRating", "Great"),
                                        GraphQLDSL.field("countryIsoCode", "USA"),
                                        GraphQLDSL.field("playerRank", 2)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);

        // Call the Query Again to hit the cache to retrieve the results
        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Below tests demonstrate using the aggregation store from dynamic configuration through JSON API.
     */
    @Test
    public void testDynamicAggregationModel() {
        String getPath = "/SalesNamespace_orderDetails?sort=customerRegion,orderTime&page[totals]&"
                        + "fields[SalesNamespace_orderDetails]=orderTotal,customerRegion,orderTime&filter=deliveryTime>=2020-01-01;deliveryTime<2020-12-31;orderTime>=2020-08";
        given()
            .when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data", hasSize(4))
            .body("data.id", hasItems("0", "1", "2", "3"))
            .body("data.attributes", hasItems(
                    allOf(hasEntry("customerRegion", "NewYork"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", null), hasEntry("orderTime", "2020-09"))))
            .body("data.attributes.orderTotal", hasItems(78.87F, 61.43F, 113.07F, 260.34F))
            .body("meta.page.number", equalTo(1))
            .body("meta.page.totalRecords", equalTo(4))
            .body("meta.page.totalPages", equalTo(1))
            .body("meta.page.limit", equalTo(500));

        // Run the query again to hit the cache.
        given()
            .when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data", hasSize(4))
            .body("data.id", hasItems("0", "1", "2", "3"))
            .body("data.attributes", hasItems(
                    allOf(hasEntry("customerRegion", "NewYork"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", "Virginia"), hasEntry("orderTime", "2020-08")),
                    allOf(hasEntry("customerRegion", null), hasEntry("orderTime", "2020-09"))))
            .body("data.attributes.orderTotal", hasItems(78.87F, 61.43F, 113.07F, 260.34F))
            .body("meta.page.number", equalTo(1))
            .body("meta.page.totalRecords", equalTo(4))
            .body("meta.page.totalPages", equalTo(1))
            .body("meta.page.limit", equalTo(500));
    }
}
