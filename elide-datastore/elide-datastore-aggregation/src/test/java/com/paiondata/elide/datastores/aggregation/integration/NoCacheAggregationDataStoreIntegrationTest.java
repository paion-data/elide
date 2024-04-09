/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.integration;

import static com.paiondata.elide.test.graphql.GraphQLDSL.argument;
import static com.paiondata.elide.test.graphql.GraphQLDSL.field;
import static com.paiondata.elide.test.graphql.GraphQLDSL.mutation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.data;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import com.paiondata.elide.core.datastore.test.DataStoreTestHarness;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.datastores.aggregation.AggregationDataStore;
import com.paiondata.elide.datastores.aggregation.framework.NoCacheAggregationDataStoreTestHarness;
import com.paiondata.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.test.graphql.elements.Arguments;
import com.paiondata.elide.test.graphql.GraphQLDSL;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;

import example.PlayerStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for {@link AggregationDataStore}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NoCacheAggregationDataStoreIntegrationTest extends AggregationDataStoreIntegrationTest {

    public NoCacheAggregationDataStoreIntegrationTest() {
        super();
    }

    @Override
    protected DataStoreTestHarness createHarness() {

        ConnectionDetails defaultConnectionDetails = createDefaultConnectionDetails();

        EntityManagerFactory emf = createEntityManagerFactory();

        Map<String, ConnectionDetails> connectionDetailsMap = createConnectionDetailsMap(defaultConnectionDetails);

        return new NoCacheAggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, VALIDATOR);
    }

    @Test
    public void testGraphQLSchema() throws IOException {
        String graphQLRequest = "{"
                + "__type(name: \"PlayerStatsWithViewEdge\") {"
                + "   name "
                + "     fields {"
                + "         name "
                + "         type {"
                + "             name"
                + "             fields {"
                + "                 name "
                + "                 type {"
                + "                     name "
                + "                     fields {"
                + "                         name"
                + "                     }"
                + "                 }"
                + "             }"
                + "         }"
                + "     }"
                + "}"
                + "}";

        String expected = loadGraphQLResponse("testGraphQLSchema.json");

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLMetdata() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "table",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("ids", Arrays.asList("playerStatsView"))
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("name"),
                                        GraphQLDSL.field("arguments",
                                                GraphQLDSL.selections(
                                                    GraphQLDSL.field("name"),
                                                    GraphQLDSL.field("type"),
                                                    GraphQLDSL.field("defaultValue")
                                                )

                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "table",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("name", "playerStatsView"),
                                        GraphQLDSL.field("arguments",
                                                GraphQLDSL.selections(
                                                    GraphQLDSL.field("name", "rating"),
                                                    GraphQLDSL.field("type", "TEXT"),
                                                    GraphQLDSL.field("defaultValue", "")
                                                ),
                                                GraphQLDSL.selections(
                                                    GraphQLDSL.field("name", "minScore"),
                                                    GraphQLDSL.field("type", "INTEGER"),
                                                    GraphQLDSL.field("defaultValue", "0")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testColumnWhichReferencesHiddenDimension() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("zipCode")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 78.87),
                                        GraphQLDSL.field("zipCode", 0)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 61.43),
                                        GraphQLDSL.field("zipCode", 10002)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 285.19),
                                        GraphQLDSL.field("zipCode", 20166)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 88.22),
                                        GraphQLDSL.field("zipCode", 20170)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testHiddenTable() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_performance",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("totalSales")
                                )
                        )
                )
        ).toQuery();

        String errorMessage = "Bad Request Body&#39;Unknown entity {SalesNamespace_performance}.&#39;";

        runQueryWithExpectedError(graphQLRequest, errorMessage);
    }

    @Test
    public void testHiddenColumn() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("zipCodeHidden")
                                )
                        )
                )
        ).toQuery();

        String errorMessage = "Validation error (FieldUndefined@[SalesNamespace_orderDetails/edges/node/zipCodeHidden]) : Field &#39;zipCodeHidden&#39; in type &#39;SalesNamespace_orderDetails&#39; is undefined";

        runQueryWithExpectedError(graphQLRequest, errorMessage);
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
    }

    @Test
    public void parameterizedGraphQLSortWithAliasTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\""),
                                        GraphQLDSL.argument("sort", "\"ratio1\"")
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
    }

    @Test
    public void parameterizedGraphQLColumnTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderRatio", "ratio1", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("numerator", "\"orderMax\""),
                                                GraphQLDSL.argument("denominator", "\"orderMax\"")
                                        )),
                                        GraphQLDSL.field("orderRatio", "ratio2", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("numerator", "\"orderMax\""),
                                                GraphQLDSL.argument("denominator", "\"orderTotal\"")
                                        )),
                                        GraphQLDSL.field("orderRatio", "ratio3", GraphQLDSL.arguments())
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("ratio1", 1.0),
                                        GraphQLDSL.field("ratio2", 0.20190379786260731),
                                        GraphQLDSL.field("ratio3", 1.0)
                                )
                        )
                )
        ).toResponse();


        runQueryWithExpectedResult(graphQLRequest, expected);
    }

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
    }

    @Test
    public void metricFormulaTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "videoGame",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"timeSpentPerSession\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent"),
                                        GraphQLDSL.field("sessions"),
                                        GraphQLDSL.field("timeSpentPerSession"),
                                        GraphQLDSL.field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "videoGame",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent", 720),
                                        GraphQLDSL.field("sessions", 60),
                                        GraphQLDSL.field("timeSpentPerSession", 12.0),
                                        GraphQLDSL.field("playerName", "Jon Doe")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent", 350),
                                        GraphQLDSL.field("sessions", 25),
                                        GraphQLDSL.field("timeSpentPerSession", 14.0),
                                        GraphQLDSL.field("playerName", "Jane Doe")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent", 300),
                                        GraphQLDSL.field("sessions", 10),
                                        GraphQLDSL.field("timeSpentPerSession", 30.0),
                                        GraphQLDSL.field("playerName", "Han")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);

        //When admin = false

        when(securityContextMock.isUserInRole("admin.user")).thenReturn(false);

        expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "videoGame",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent", 720),
                                        GraphQLDSL.field("sessions", 60),
                                        GraphQLDSL.field("timeSpentPerSession", 12.0),
                                        GraphQLDSL.field("playerName", "Jon Doe")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent", 350),
                                        GraphQLDSL.field("sessions", 25),
                                        GraphQLDSL.field("timeSpentPerSession", 14.0),
                                        GraphQLDSL.field("playerName", "Jane Doe")
                                )
                        )
                )
        ).toResponse();
        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test sql expression in where, sorting, group by and projection.
     * @throws Exception exception
     */
    @Test
    public void dimensionFormulaTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"playerLevel\""),
                                        GraphQLDSL.argument("filter", "\"playerLevel>\\\"0\\\"\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore"),
                                        GraphQLDSL.field("playerLevel")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 1234),
                                        GraphQLDSL.field("playerLevel", 1)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 3147483647L),
                                        GraphQLDSL.field("playerLevel", 2)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void noMetricQueryTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStatsWithView",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"countryViewViewIsoCode\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("countryViewViewIsoCode")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStatsWithView",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("countryViewViewIsoCode", "HKG")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("countryViewViewIsoCode", "USA")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void whereFilterTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"overallRating==\\\"Good\\\"\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore"),
                                        GraphQLDSL.field("overallRating")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 1234),
                                        GraphQLDSL.field("overallRating", "Good")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void havingFilterTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"lowScore<\\\"45\\\"\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore"),
                                        GraphQLDSL.field("overallRating"),
                                        GraphQLDSL.field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 35),
                                        GraphQLDSL.field("overallRating", "Good"),
                                        GraphQLDSL.field("playerName", "Jon Doe")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test the case that a where clause is promoted into having clause.
     * @throws Exception exception
     */
    @Test
    public void wherePromotionTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"overallRating==\\\"Good\\\",lowScore<\\\"45\\\"\""),
                                        GraphQLDSL.argument("sort", "\"lowScore\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore"),
                                        GraphQLDSL.field("overallRating"),
                                        GraphQLDSL.field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 35),
                                        GraphQLDSL.field("overallRating", "Good"),
                                        GraphQLDSL.field("playerName", "Jon Doe")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 72),
                                        GraphQLDSL.field("overallRating", "Good"),
                                        GraphQLDSL.field("playerName", "Han")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test the case that a where clause, which requires dimension join, is promoted into having clause.
     * @throws Exception exception
     */
    @Test
    public void havingClauseJoinTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"countryIsoCode==\\\"USA\\\",lowScore<\\\"45\\\"\""),
                                        GraphQLDSL.argument("sort", "\"lowScore\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore"),
                                        GraphQLDSL.field("countryIsoCode"),
                                        GraphQLDSL.field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 35),
                                        GraphQLDSL.field("countryIsoCode", "USA"),
                                        GraphQLDSL.field("playerName", "Jon Doe")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 241),
                                        GraphQLDSL.field("countryIsoCode", "USA"),
                                        GraphQLDSL.field("playerName", "Jane Doe")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test invalid where promotion on a dimension field that is not grouped.
     * @throws Exception exception
     */
    @Test
    public void ungroupedHavingDimensionTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"countryIsoCode==\\\"USA\\\",lowScore<\\\"45\\\"\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String errorMessage = "Exception while fetching data (/playerStats) : Invalid operation: "
                + "Post aggregation filtering on &#39;countryIsoCode&#39; requires the field to be projected in the response";

        runQueryWithExpectedError(graphQLRequest, errorMessage);
    }

    /**
     * Test invalid having clause on a metric field that is not aggregated.
     * @throws Exception exception
     */
    @Test
    public void nonAggregatedHavingMetricTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"highScore>\\\"0\\\"\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 35)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Test invalid where promotion on a different class than the queried class.
     * @throws Exception exception
     */
    @Test
    public void invalidHavingClauseClassTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"country.isoCode==\\\"USA\\\",lowScore<\\\"45\\\"\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String errorMessage = "Exception while fetching data (/playerStats) : Invalid operation: "
                + "Relationship traversal not supported for analytic queries.";

        runQueryWithExpectedError(graphQLRequest, errorMessage);
    }

    @Test
    public void dimensionSortingTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"overallRating\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore"),
                                        GraphQLDSL.field("overallRating")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 35),
                                        GraphQLDSL.field("overallRating", "Good")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 241),
                                        GraphQLDSL.field("overallRating", "Great")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void metricSortingTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"-highScore\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore"),
                                        GraphQLDSL.field("countryIsoCode")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 3147483647L),
                                        GraphQLDSL.field("countryIsoCode", "USA")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 1000),
                                        GraphQLDSL.field("countryIsoCode", "HKG")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void multipleColumnsSortingTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"overallRating,playerName\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore"),
                                        GraphQLDSL.field("overallRating"),
                                        GraphQLDSL.field("playerName")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 72),
                                        GraphQLDSL.field("overallRating", "Good"),
                                        GraphQLDSL.field("playerName", "Han")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 35),
                                        GraphQLDSL.field("overallRating", "Good"),
                                        GraphQLDSL.field("playerName", "Jon Doe")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore", 241),
                                        GraphQLDSL.field("overallRating", "Great"),
                                        GraphQLDSL.field("playerName", "Jane Doe")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void idSortingTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"id\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore"),
                                        GraphQLDSL.field("id")
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Sorting on id field is not permitted";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void nestedDimensionNotInQuerySortingTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"-countryIsoCode,lowScore\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore")
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Can not sort on countryIsoCode as it is not present in query";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void sortingOnMetricNotInQueryTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"highScore\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("lowScore"),
                                        GraphQLDSL.field("countryIsoCode")
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Can not sort on highScore as it is not present in query";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void basicViewAggregationTest() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStatsWithView",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"highScore\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore"),
                                        GraphQLDSL.field("countryViewIsoCode")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStatsWithView",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 1000),
                                        GraphQLDSL.field("countryViewIsoCode", "HKG")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 3147483647L),
                                        GraphQLDSL.field("countryViewIsoCode", "USA")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void multiTimeDimensionTest() throws IOException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("recordedDate"),
                                        GraphQLDSL.field("updatedDate")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("recordedDate", "2019-07-11"),
                                        GraphQLDSL.field("updatedDate", "2020-07-12")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("recordedDate", "2019-07-12"),
                                        GraphQLDSL.field("updatedDate", "2019-10-12")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("recordedDate", "2019-07-13"),
                                        GraphQLDSL.field("updatedDate", "2020-01-12")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphqlQueryDynamicModelById() throws IOException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("orderTotal")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "0"),
                                        GraphQLDSL.field("orderTotal", 513.71)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void jsonApiAggregationTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/playerStats")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", hasItems("0", "1", "2"))
                .body("data.attributes.highScore", hasItems(1000, 1234, 3147483647L))
                .body("data.attributes.countryIsoCode", hasItems("USA", "HKG"));
    }

    /**
     * Below tests demonstrate using the aggregation store from dynamic configuration.
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
    }

    @Test
    public void testInvalidSparseFields() {
        String expectedError = "Invalid value: SalesNamespace_orderDetails does not contain the fields: [orderValue, customerState]";
        String getPath = "/SalesNamespace_orderDetails?fields[SalesNamespace_orderDetails]=orderValue,customerState,orderTime";
        given()
            .when()
            .get(getPath)
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body("errors.detail", hasItems(expectedError));
    }

    @Test
    public void missingClientFilterTest() {
        String expectedError = "Querying SalesNamespace_orderDetails requires a mandatory filter:"
                + " deliveryTime&gt;={{start}};deliveryTime&lt;{{end}}";
        when()
        .get("/SalesNamespace_orderDetails/")
        .then()
        .body("errors.detail", hasItems(expectedError))
        .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void incompleteClientFilterTest() {
        String expectedError = "Querying SalesNamespace_orderDetails requires a mandatory filter:"
                + " deliveryTime&gt;={{start}};deliveryTime&lt;{{end}}";
        when()
        .get("/SalesNamespace_orderDetails?filter=deliveryTime>=2020-08")
        .then()
        .body("errors.detail", hasItems(expectedError))
        .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void completeClientFilterTest() {
        when()
        .get("/SalesNamespace_deliveryDetails?filter=month>=2020-08;month<2020-09")
        .then()
        .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testGraphQLDynamicAggregationModel() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31';orderTime=='2020-08'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("customerRegionRegion"),
                                        GraphQLDSL.field("orderTime", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH)
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
                                        GraphQLDSL.field("orderTotal", 61.43F),
                                        GraphQLDSL.field("customerRegion", "NewYork"),
                                        GraphQLDSL.field("customerRegionRegion", "NewYork"),
                                        GraphQLDSL.field("orderTime", "2020-08")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 113.07F),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTime", "2020-08")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Tests for below type of column references.
     *
     * a) Physical Column Reference in same table.
     * b) Logical Column Reference in same table, which references Physical column in same table.
     * c) Logical Column Reference in same table, which references another Logical column in same table, which
     *  references Physical column in same table.
     * d) Physical Column Reference in referred table.
     * e) Logical Column Reference in referred table, which references Physical column in referred table.
     * f) Logical Column Reference in referred table, which references another Logical column in referred table, which
     *  references another Logical column in referred table, which references Physical column in referred table.
     * g) Logical Column Reference in same table, which references Physical column in referred table.
     * h) Logical Column Reference in same table, which references another Logical Column in referred table, which
     *  references another Logical column in referred table, which references another Logical column in referred table,
     *  which references Physical column in referred table
     *
     * @throws Exception
     */
    @Test
    public void testGraphQLDynamicAggregationModelAllFields() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"courierName,deliveryDate,orderTotal,customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"deliveryYear=='2020';(deliveryTime>='2020-08-01';deliveryTime<'2020-12-31');(deliveryDate>='2020-09-01',orderTotal>50)\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName"),
                                        GraphQLDSL.field("deliveryTime"),
                                        GraphQLDSL.field("deliveryHour"),
                                        GraphQLDSL.field("deliveryDate"),
                                        GraphQLDSL.field("deliveryMonth"),
                                        GraphQLDSL.field("deliveryYear"),
                                        GraphQLDSL.field("deliveryDefault"),
                                        GraphQLDSL.field("orderTime", "bySecond", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.SECOND)
                                        )),
                                        GraphQLDSL.field("orderTime", "byDay", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.DAY)
                                        )),
                                        GraphQLDSL.field("orderTime", "byMonth", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH)
                                        )),
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("customerRegionRegion"),
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("zipCode"),
                                        GraphQLDSL.field("orderId")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "FEDEX"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-11T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-11T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-11"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-09-08T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-11"),
                                        GraphQLDSL.field("byDay", "2020-09-08"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 84.11F),
                                        GraphQLDSL.field("zipCode", 20166),
                                        GraphQLDSL.field("orderId", "order-1b")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "FEDEX"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-11T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-11T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-11"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-09-08T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-11"),
                                        GraphQLDSL.field("byDay", "2020-09-08"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 97.36F),
                                        GraphQLDSL.field("zipCode", 20166),
                                        GraphQLDSL.field("orderId", "order-1c")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "UPS"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-05T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-05T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-05"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-08-30T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-05"),
                                        GraphQLDSL.field("byDay", "2020-08-30"),
                                        GraphQLDSL.field("byMonth", "2020-08"),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 103.72F),
                                        GraphQLDSL.field("zipCode", 20166),
                                        GraphQLDSL.field("orderId", "order-1a")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "UPS"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-13T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-13T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-13"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-09-09T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-13"),
                                        GraphQLDSL.field("byDay", "2020-09-09"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("customerRegion", (String) null, false),
                                        GraphQLDSL.field("customerRegionRegion", (String) null, false),
                                        GraphQLDSL.field("orderTotal", 78.87F),
                                        GraphQLDSL.field("zipCode", 0),
                                        GraphQLDSL.field("orderId", "order-null-enum")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "UPS"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-13T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-13T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-13"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-09-09T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-13"),
                                        GraphQLDSL.field("byDay", "2020-09-09"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 78.87F),
                                        GraphQLDSL.field("zipCode", 20170),
                                        GraphQLDSL.field("orderId", "order-3b")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Verifies tableMaker logic.  Duplicates everything query for orderDetails (no maker) on
     * orderDetails2 (maker).
     * @throws Exception
     */
    @Test
    public void testTableMaker() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails2",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"courierName,deliveryDate,orderTotal,customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"deliveryYear=='2020';(deliveryTime>='2020-08-01';deliveryTime<'2020-12-31');(deliveryDate>='2020-09-01',orderTotal>50)\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName"),
                                        GraphQLDSL.field("deliveryTime"),
                                        GraphQLDSL.field("deliveryHour"),
                                        GraphQLDSL.field("deliveryDate"),
                                        GraphQLDSL.field("deliveryMonth"),
                                        GraphQLDSL.field("deliveryYear"),
                                        GraphQLDSL.field("deliveryDefault"),
                                        GraphQLDSL.field("orderTime", "bySecond", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.SECOND)
                                        )),
                                        GraphQLDSL.field("orderTime", "byDay", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.DAY)
                                        )),
                                        GraphQLDSL.field("orderTime", "byMonth", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH)
                                        )),
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("customerRegionRegion"),
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("zipCode"),
                                        GraphQLDSL.field("orderId")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails2",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "FEDEX"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-11T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-11T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-11"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-09-08T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-11"),
                                        GraphQLDSL.field("byDay", "2020-09-08"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 84.11F),
                                        GraphQLDSL.field("zipCode", 20166),
                                        GraphQLDSL.field("orderId", "order-1b")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "FEDEX"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-11T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-11T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-11"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-09-08T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-11"),
                                        GraphQLDSL.field("byDay", "2020-09-08"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 97.36F),
                                        GraphQLDSL.field("zipCode", 20166),
                                        GraphQLDSL.field("orderId", "order-1c")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "UPS"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-05T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-05T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-05"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-08-30T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-05"),
                                        GraphQLDSL.field("byDay", "2020-08-30"),
                                        GraphQLDSL.field("byMonth", "2020-08"),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 103.72F),
                                        GraphQLDSL.field("zipCode", 20166),
                                        GraphQLDSL.field("orderId", "order-1a")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "UPS"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-13T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-13T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-13"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-09-09T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-13"),
                                        GraphQLDSL.field("byDay", "2020-09-09"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("customerRegion", (String) null, false),
                                        GraphQLDSL.field("customerRegionRegion", (String) null, false),
                                        GraphQLDSL.field("orderTotal", 78.87F),
                                        GraphQLDSL.field("zipCode", 0),
                                        GraphQLDSL.field("orderId", "order-null-enum")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("courierName", "UPS"),
                                        GraphQLDSL.field("deliveryTime", "2020-09-13T16:30:11"),
                                        GraphQLDSL.field("deliveryHour", "2020-09-13T16"),
                                        GraphQLDSL.field("deliveryDate", "2020-09-13"),
                                        GraphQLDSL.field("deliveryMonth", "2020-09"),
                                        GraphQLDSL.field("deliveryYear", "2020"),
                                        GraphQLDSL.field("bySecond", "2020-09-09T16:30:11"),
                                        GraphQLDSL.field("deliveryDefault", "2020-09-13"),
                                        GraphQLDSL.field("byDay", "2020-09-09"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("customerRegionRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 78.87F),
                                        GraphQLDSL.field("zipCode", 20170),
                                        GraphQLDSL.field("orderId", "order-3b")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLDynamicAggregationModelDateTime() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"bySecond=='2020-09-08T16:30:11';(deliveryTime>='2020-01-01';deliveryTime<'2020-12-31')\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("orderTime", "byMonth", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH)
                                        )),
                                        GraphQLDSL.field("orderTime", "bySecond", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.SECOND)
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
                                        GraphQLDSL.field("orderTotal", 181.47F),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("byMonth", "2020-09"),
                                        GraphQLDSL.field("bySecond", "2020-09-08T16:30:11")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimMismatchArgs() throws Exception {

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"orderTime[grain:DAY]=='2020-08',orderTotal>50\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("orderTime", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH) // Does not match grain argument in filter
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/SalesNamespace_orderDetails) : Invalid operation: Post aggregation filtering on &#39;orderTime&#39; requires the field to be projected in the response with matching arguments";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimMismatchArgsWithDefaultSelect() throws Exception {

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"orderTime[grain:DAY]=='2020-08',orderTotal>50\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("orderTime") //Default Grain for OrderTime is Month.
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/SalesNamespace_orderDetails) : Invalid operation: Post aggregation filtering on &#39;orderTime&#39; requires the field to be projected in the response with matching arguments";


        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimMismatchArgsWithDefaultFilter() throws Exception {

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"orderTime,customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"(orderTime=='2020-08-01',orderTotal>50);(deliveryTime>='2020-01-01';deliveryTime<'2020-12-31')\"") //No Grain Arg passed, so works based on Alias's argument in Selection.
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("orderTime", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.DAY)
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
                                        GraphQLDSL.field("orderTotal", 103.72F),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("orderTime", "2020-08-30")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 181.47F),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("orderTime", "2020-09-08")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 78.87F),
                                        GraphQLDSL.field("customerRegion", (String) null, false),
                                        GraphQLDSL.field("orderTime", "2020-09-09")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 78.87F),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("orderTime", "2020-09-09")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testAdminRole() throws Exception {

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31';orderTime=='2020-08'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("orderTime", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 61.43F),
                                        GraphQLDSL.field("customerRegion", "NewYork"),
                                        GraphQLDSL.field("orderTime", "2020-08")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal", 113.07F),
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("orderTime", "2020-08")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testOperatorRole() throws Exception {

        when(securityContextMock.isUserInRole("admin")).thenReturn(false);

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31';orderTime=='2020-08'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("orderTime", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegion", "NewYork"),
                                        GraphQLDSL.field("orderTime", "2020-08")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("orderTime", "2020-08")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGuestUserRole() throws Exception {

        when(securityContextMock.isUserInRole("admin")).thenReturn(false);
        when(securityContextMock.isUserInRole("operator")).thenReturn(false);

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31';orderTime=='2020-08'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("orderTime", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/SalesNamespace_orderDetails/edges[0]/node/customerRegion) : ReadPermission Denied";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimensionAliases() throws Exception {

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"byDay>='2019-07-12'\""),
                                        GraphQLDSL.argument("sort", "\"byDay\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore"),
                                        GraphQLDSL.field("recordedDate", "byDay", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.DAY)
                                        )),
                                        GraphQLDSL.field("recordedDate", "byMonth", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.MONTH)
                                        )),
                                        GraphQLDSL.field("recordedDate", "byQuarter", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.QUARTER)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 1234),
                                        GraphQLDSL.field("byDay", "2019-07-12"),
                                        GraphQLDSL.field("byMonth", "2019-07"),
                                        GraphQLDSL.field("byQuarter", "2019-07")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 1000),
                                        GraphQLDSL.field("byDay", "2019-07-13"),
                                        GraphQLDSL.field("byMonth", "2019-07"),
                                        GraphQLDSL.field("byQuarter", "2019-07")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Check if AggregationBeforeJoinOptimizer works with alias.
     * @throws Exception
     */
    @Test
    public void testJoinBeforeAggregationWithAlias() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"highScore\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore"),
                                        GraphQLDSL.field("countryIsoCode", "countryAlias", Arguments.emptyArgument())
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
                                        GraphQLDSL.field("countryAlias", "HKG")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", 3147483647L),
                                        GraphQLDSL.field("countryAlias", "USA")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    /**
     * Check working of alias on simple metrics, 2-pass agg metrics, simple dimensions, join dimension, and date dimension.
     *
     * Note that Optimizer is not invoked because of 2 pass aggregation metrics.
     * @throws Exception
     */
    @Test
    public void testMetricsAndDimensionsWithAlias() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScore", "highScoreAlias", Arguments.emptyArgument()),
                                        GraphQLDSL.field("dailyAverageScorePerPeriod", "avgScoreAlias", Arguments.emptyArgument()),
                                        GraphQLDSL.field("overallRating", "ratingAlias", Arguments.emptyArgument()),
                                        GraphQLDSL.field("countryIsoCode", "countryAlias", Arguments.emptyArgument()),
                                        GraphQLDSL.field("recordedDate", "byDay", GraphQLDSL.arguments(
                                                GraphQLDSL.argument("grain", TimeGrain.DAY)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScoreAlias", 1000),
                                        GraphQLDSL.field("avgScoreAlias", 1000.0),
                                        GraphQLDSL.field("ratingAlias", "Good"),
                                        GraphQLDSL.field("countryAlias", "HKG"),
                                        GraphQLDSL.field("byDay", "2019-07-13")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScoreAlias", 1234),
                                        GraphQLDSL.field("avgScoreAlias", 1234),
                                        GraphQLDSL.field("ratingAlias", "Good"),
                                        GraphQLDSL.field("countryAlias", "USA"),
                                        GraphQLDSL.field("byDay", "2019-07-12")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("highScoreAlias", 3147483647L),
                                        GraphQLDSL.field("avgScoreAlias", 3147483647L),
                                        GraphQLDSL.field("ratingAlias", "Great"),
                                        GraphQLDSL.field("countryAlias", "USA"),
                                        GraphQLDSL.field("byDay", "2019-07-11")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testTimeDimensionArgumentsInFilter() throws Exception {

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"customerRegion\""),
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31';orderTime[grain:day]=='2020-09-08'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegion"),
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("orderTime", GraphQLDSL.arguments(
                                                   GraphQLDSL.argument("grain", TimeGrain.MONTH)
                                        ))
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegion", "Virginia"),
                                        GraphQLDSL.field("orderTotal", 181.47F),
                                        GraphQLDSL.field("orderTime", "2020-09")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testSchemaIntrospection() throws Exception {
        String graphQLRequest = "{"
                + "__schema {"
                + "   mutationType {"
                + "     name "
                + "     fields {"
                + "       name "
                + "       args {"
                + "          name"
                + "          defaultValue"
                + "       }"
                + "     }"
                + "   }"
                + "}"
                + "}";

        String query = toJsonQuery(graphQLRequest, new HashMap<>());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(query)
            .post("/graphQL")
            .then()
            .statusCode(HttpStatus.SC_OK)
            // Verify that the SalesNamespace_orderDetails Model has an argument "denominator".
            .body("data.__schema.mutationType.fields.find { it.name == 'SalesNamespace_orderDetails' }.args.name[7] ", equalTo("denominator"));

        graphQLRequest = "{"
                + "__type(name: \"SalesNamespace_orderDetails\") {"
                + "   name"
                + "   fields {"
                + "     name "
                + "     args {"
                + "        name"
                + "        defaultValue"
                + "     }"
                + "   }"
                + "}"
                + "}";

        query = toJsonQuery(graphQLRequest, new HashMap<>());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(query)
            .post("/graphQL")
            .then()
            .statusCode(HttpStatus.SC_OK)
            // Verify that the orderTotal attribute has an argument "precision".
            .body("data.__type.fields.find { it.name == 'orderTotal' }.args.name[0]", equalTo("precision"));
    }

    @Test
    public void testDelete() throws IOException {
        String graphQLRequest = GraphQLDSL.mutation(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("op", "DELETE"),
                                        GraphQLDSL.argument("ids", Arrays.asList("0"))
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("overallRating")
                                )
                        )
                )
        ).toGraphQLSpec();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Filtering by ID is not supported on playerStats";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testUpdate() throws IOException {

        PlayerStats playerStats = new PlayerStats();
        playerStats.setId("1");
        playerStats.setHighScore(100);

        String graphQLRequest = GraphQLDSL.mutation(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("op", "UPDATE"),
                                        GraphQLDSL.argument("data", playerStats)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("overallRating")
                                )
                        )
                )
        ).toGraphQLSpec();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Filtering by ID is not supported on playerStats";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testUpsertWithStaticModel() throws IOException {

        PlayerStats playerStats = new PlayerStats();
        playerStats.setId("1");
        playerStats.setHighScore(100);

        String graphQLRequest = GraphQLDSL.mutation(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("op", "UPSERT"),
                                        GraphQLDSL.argument("data", playerStats)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("overallRating")
                                )
                        )
                )
        ).toGraphQLSpec();

        String expected = "Exception while fetching data (/playerStats) : Invalid operation: Filtering by ID is not supported on playerStats";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    @Test
    public void testUpsertWithDynamicModel() throws IOException {

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "1");
        order.put("courierName", "foo");

        String graphQLRequest = GraphQLDSL.mutation(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("op", "UPSERT"),
                                        GraphQLDSL.argument("data", order)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderId")
                                )
                        )
                )
        ).toGraphQLSpec();

        String expected = "Invalid operation: UPSERT is not permitted on SalesNamespace_orderDetails.";

        runQueryWithExpectedError(graphQLRequest, expected);
    }

    /**
     * Test missing required column filter on deliveryYear.
     * @throws Exception exception
     */
    @Test
    public void missingRequiredColumnFilter() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(

                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("orderTotal"),
                                        GraphQLDSL.field("deliveryYear")
                                )
                        )
                )
        ).toQuery();

        String errorMessage = "Exception while fetching data (/SalesNamespace_orderDetails) : "
                + "Querying deliveryYear requires a mandatory filter: deliveryYear=={{deliveryYear}}";

        runQueryWithExpectedError(graphQLRequest, errorMessage);
    }

    //Security
    @Test
    public void testPermissionFilters() throws IOException {
        when(securityContextMock.isUserInRole("admin.user")).thenReturn(false);

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "videoGame",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"timeSpentPerSession\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent"),
                                        GraphQLDSL.field("sessions"),
                                        GraphQLDSL.field("timeSpentPerSession")
                                )
                        )
                )
        ).toQuery();

        //Records for Jon Doe and Jane Doe will only be aggregated.
        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "videoGame",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent", 1070),
                                        GraphQLDSL.field("sessions", 85),
                                        GraphQLDSL.field("timeSpentPerSession", 12.588235)
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);

    }

    @Test
    public void testFieldPermissions() throws IOException {
        when(securityContextMock.isUserInRole("operator")).thenReturn(false);
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "videoGame",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("timeSpent"),
                                        GraphQLDSL.field("sessions"),
                                        GraphQLDSL.field("timeSpentPerSession"),
                                        GraphQLDSL.field("timeSpentPerGame")
                                )
                        )
                )
        ).toQuery();

        String expected = "Exception while fetching data (/videoGame/edges[0]/node/timeSpentPerGame) : ReadPermission Denied";

        runQueryWithExpectedError(graphQLRequest, expected);

    }

    @Test
    public void testEnumDimension() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegionType1"),
                                        GraphQLDSL.field("customerRegionType2")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegionType1", (String) null, false),
                                        GraphQLDSL.field("customerRegionType2", (String) null, false)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegionType1", "STATE"),
                                        GraphQLDSL.field("customerRegionType2", "STATE")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testHjsonFilterByEnumDimension() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31';customerRegionType1==STATE;customerRegionType2==STATE\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegionType1"),
                                        GraphQLDSL.field("customerRegionType2")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegionType1", "STATE"),
                                        GraphQLDSL.field("customerRegionType2", "STATE")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testJavaFilterByEnumDimension() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"placeType1==STATE;placeType2==STATE\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("placeType1"),
                                        GraphQLDSL.field("placeType2")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("placeType1", "STATE"),
                                        GraphQLDSL.field("placeType2", "STATE")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testJavaSortByEnumDimension() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("sort", "\"placeType1,placeType2\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("placeType1"),
                                        GraphQLDSL.field("placeType2")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "playerStats",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("placeType1", "STATE"),
                                        GraphQLDSL.field("placeType2", "STATE")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testHjsonSortByEnumDimension() throws Exception {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("filter", "\"deliveryTime>='2020-01-01';deliveryTime<'2020-12-31'\""),
                                        GraphQLDSL.argument("sort", "\"customerRegionType1,customerRegionType2\"")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegionType1"),
                                        GraphQLDSL.field("customerRegionType2")
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "SalesNamespace_orderDetails",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegionType1", (String) null, false),
                                        GraphQLDSL.field("customerRegionType2", (String) null, false)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("customerRegionType1", "STATE"),
                                        GraphQLDSL.field("customerRegionType2", "STATE")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }
}
