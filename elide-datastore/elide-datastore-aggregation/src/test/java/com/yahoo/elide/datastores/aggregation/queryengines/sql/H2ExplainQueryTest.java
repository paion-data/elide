/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.datastores.aggregation.framework.SQLUnitTest;
import com.paiondata.elide.datastores.aggregation.query.Query;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class tests SQLQueryEngine.explain() with the H2 dialect.
 */
public class H2ExplainQueryTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        SQLUnitTest.init(SQLDialectFactory.getH2Dialect());
    }

//    TODO - Should this generate an error from the engine level?
//    @Test
//    public void testExplainNoMetricsOrDimensions() {
//        Query query = Query.builder()
//                .table(playerStatsTable)
//                .build();
//        String expectedQueryStr = "SELECT DISTINCT  " +
//                "FROM playerStats AS example_PlayerStats";
//
//        compareQueryLists(expectedQueryStr, engine.explain(query));
//    }

    @Test
    public void testExplainWhereDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT `example_PlayerStats`.`overallRating` AS `overallRating` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "WHERE `example_PlayerStats`.`overallRating` IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.explain(TestQuery.WHERE_DIMS_ONLY.getQuery()));

        testQueryExecution(TestQuery.WHERE_DIMS_ONLY.getQuery());
    }

    @Test
    public void testExplainWhereAnd() throws Exception {
        Query query = TestQuery.WHERE_AND.getQuery();
        String expectedQueryStr =
                "SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "`example_PlayerStats`.`overallRating` AS `overallRating` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "LEFT OUTER JOIN `countries` AS `example_PlayerStats_country_XXX` "
                        + "ON `example_PlayerStats`.`country_id` = `example_PlayerStats_country_XXX`.`id` "
                        + "WHERE (`example_PlayerStats`.`overallRating` IS NOT NULL AND `example_PlayerStats_country_XXX`.`iso_code` IN (:XXX)) "
                        + " GROUP BY `example_PlayerStats`.`overallRating`\n";

        compareQueryLists(expectedQueryStr, engine.explain(query));

        testQueryExecution(TestQuery.WHERE_AND.getQuery());
    }

    @Test
    public void textExplainWhereOr() throws Exception {
        Query query = TestQuery.WHERE_OR.getQuery();
        String expectedQueryStr =
                "SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "`example_PlayerStats`.`overallRating` AS `overallRating` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "LEFT OUTER JOIN `countries` AS `example_PlayerStats_country_XXX` "
                        + "ON `example_PlayerStats`.`country_id` = `example_PlayerStats_country_XXX`.`id` "
                        + "WHERE (`example_PlayerStats`.`overallRating` IS NOT NULL OR `example_PlayerStats_country_XXX`.`iso_code` IN (:XXX)) "
                        + " GROUP BY `example_PlayerStats`.`overallRating`\n";

        compareQueryLists(expectedQueryStr, engine.explain(query));

        testQueryExecution(TestQuery.WHERE_OR.getQuery());
    }

    @Test
    public void testExplainHavingMetricsOnly() throws Exception {
        Query query = TestQuery.HAVING_METRICS_ONLY.getQuery();
        String expectedQueryStr =
                "SELECT MIN(`example_PlayerStats`.`lowScore`) AS `lowScore` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "HAVING MIN(`example_PlayerStats`.`lowScore`) > :XXX";
        compareQueryLists(expectedQueryStr, engine.explain(query));

        testQueryExecution(TestQuery.HAVING_METRICS_ONLY.getQuery());
    }

    @Test
    public void testExplainHavingDimsOnly() throws Exception {
        String expectedQueryStr =
                "SELECT DISTINCT `example_PlayerStats`.`overallRating` AS `overallRating` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "HAVING `example_PlayerStats`.`overallRating` IS NOT NULL";
        compareQueryLists(expectedQueryStr, engine.explain(TestQuery.HAVING_DIMS_ONLY.getQuery()));

        //H2 does not allow HAVING on a column not in the GROUP BY list.
        //testQueryExecution(TestQuery.HAVING_DIMS_ONLY.getQuery());
    }

    @Test
    public void testExplainHavingMetricsAndDims() throws Exception {
        Query query = TestQuery.HAVING_METRICS_AND_DIMS.getQuery();

        String expectedQueryStr =
                "SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "`example_PlayerStats`.`overallRating` AS `overallRating` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "GROUP BY `example_PlayerStats`.`overallRating` "
                        + "HAVING (`example_PlayerStats`.`overallRating` IS NOT NULL "
                        + "AND MAX(`example_PlayerStats`.`highScore`) > :XXX)";
        compareQueryLists(expectedQueryStr, engine.explain(query));

        testQueryExecution(TestQuery.HAVING_METRICS_AND_DIMS.getQuery());
    }

    @Test
    public void testExplainHavingMetricsOrDims() throws Exception {
        Query query = TestQuery.HAVING_METRICS_OR_DIMS.getQuery();

        String expectedQueryStr =
                "SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "`example_PlayerStats`.`overallRating` AS `overallRating` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "GROUP BY `example_PlayerStats`.`overallRating` "
                        + "HAVING (`example_PlayerStats`.`overallRating` IS NOT NULL "
                        + "OR MAX(`example_PlayerStats`.`highScore`) > :XXX)";
        compareQueryLists(expectedQueryStr, engine.explain(query));

        testQueryExecution(TestQuery.HAVING_METRICS_OR_DIMS.getQuery());
    }

    @Test
    public void testExplainPagination() {
        String expectedQueryStr1 = "SELECT COUNT(*) FROM "
                        + "(SELECT `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "GROUP BY `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') ) AS `pagination_subquery`";

        String expectedQueryStr2 =
                "SELECT MIN(`example_PlayerStats`.`lowScore`) AS "
                        + "`lowScore`,`example_PlayerStats`.`overallRating` AS "
                        + "`overallRating`,PARSEDATETIME(FORMATDATETIME("
                        + "`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS `recordedDate` FROM `playerStats` AS "
                        + "`example_PlayerStats`   "
                        + "GROUP BY `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') LIMIT 1 OFFSET 0";
        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.PAGINATION_TOTAL.getQuery()));

        testQueryExecution(TestQuery.PAGINATION_TOTAL.getQuery());
    }

    @Test
    public void testExplainSortingAscending() {
        String expectedQueryStr =
                "SELECT MIN(`example_PlayerStats`.`lowScore`) AS `lowScore` "
                        + "FROM `playerStats` AS `example_PlayerStats`   "
                        + "ORDER BY MIN(`example_PlayerStats`.`lowScore`) ASC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_ASC.getQuery()));

        testQueryExecution(TestQuery.SORT_METRIC_ASC.getQuery());
    }

    @Test
    public void testExplainSortingDecending() {
        String expectedQueryStr =
                "SELECT MIN(`example_PlayerStats`.`lowScore`) AS `lowScore` "
                        + "FROM `playerStats` AS `example_PlayerStats`   "
                        + "ORDER BY MIN(`example_PlayerStats`.`lowScore`) DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_DESC.getQuery()));

        testQueryExecution(TestQuery.SORT_METRIC_DESC.getQuery());
    }

    @Test
    public void testExplainSortingByDimensionDesc() {
        String expectedQueryStr =
                "SELECT DISTINCT `example_PlayerStats`.`overallRating` AS "
                        + "`overallRating` FROM `playerStats` AS `example_PlayerStats` "
                        + "ORDER BY `example_PlayerStats`.`overallRating` DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_DIM_DESC.getQuery()));

        testQueryExecution(TestQuery.SORT_DIM_DESC.getQuery());
    }

    @Test
    public void testExplainSortingByMetricAndDimension() {
        String expectedQueryStr =
                "SELECT MAX(`example_PlayerStats`.`highScore`) "
                        + "AS `highScore`,`example_PlayerStats`.`overallRating` AS "
                        + "`overallRating` FROM `playerStats` AS `example_PlayerStats` "
                        + "GROUP BY `example_PlayerStats`.`overallRating` "
                        + "ORDER BY MAX(`example_PlayerStats`.`highScore`) DESC,"
                        + "`example_PlayerStats`.`overallRating` DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SORT_METRIC_AND_DIM_DESC.getQuery()));

        testQueryExecution(TestQuery.SORT_METRIC_AND_DIM_DESC.getQuery());
    }

    @Test
    public void testExplainSelectFromSubquery() {
        String expectedQueryStr =
                "SELECT MAX(`example_PlayerStatsView`.`highScore`) AS "
                        + "`highScore` FROM (SELECT stats.highScore, stats.player_id, c.name as countryName FROM "
                        + "playerStats AS stats LEFT JOIN countries AS c ON stats.country_id = c.id "
                        + "WHERE stats.overallRating = 'Great' AND stats.highScore >= 0) AS "
                        + "`example_PlayerStatsView`";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.SUBQUERY.getQuery()));

        testQueryExecution(TestQuery.SUBQUERY.getQuery());
    }

    @Test
    public void testExplainOrderByNotInSelect() {
        String expectedQueryStr =
                "SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "ORDER BY `example_PlayerStats`.`overallRating` DESC";
        List<String> expectedQueryList = Arrays.asList(expectedQueryStr);
        compareQueryLists(expectedQueryList, engine.explain(TestQuery.ORDER_BY_DIMENSION_NOT_IN_SELECT.getQuery()));

        //H2 does not allow ORDER BY on a column not in the GROUP BY list.
        //testQueryExecution(TestQuery.ORDER_BY_DIMENSION_NOT_IN_SELECT.getQuery());
    }

    @Test
    public void testExplainComplicated() {
        Query query = TestQuery.COMPLICATED.getQuery();

        String expectedQueryStr1 = "SELECT COUNT(*) FROM "
                        + "(SELECT `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "LEFT OUTER JOIN `countries` AS `example_PlayerStats_country_XXX` "
                        + "ON `example_PlayerStats`.`country_id` = "
                        + "`example_PlayerStats_country_XXX`.`id` "
                        + "WHERE `example_PlayerStats_country_XXX`.`iso_code` "
                        + "IN (:XXX) "
                        + "GROUP BY `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MIN(`example_PlayerStats`.`lowScore`) > :XXX ) AS `pagination_subquery`";

        String expectedQueryStr2 =
                "SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                        + "`example_PlayerStats`.`overallRating` AS `overallRating`,"
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), "
                        + "'yyyy-MM-dd') AS `recordedDate` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "LEFT OUTER JOIN `countries` AS `example_PlayerStats_country_XXX` "
                        + "ON `example_PlayerStats`.`country_id` = "
                        + "`example_PlayerStats_country_XXX`.`id` "
                        + "WHERE `example_PlayerStats_country_XXX`.`iso_code` "
                        + "IN (:XXX) "
                        + "GROUP BY `example_PlayerStats`.`overallRating`, "
                        + "PARSEDATETIME(FORMATDATETIME("
                        + "`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') "
                        + "HAVING MIN(`example_PlayerStats`.`lowScore`) > :XXX "
                        + "ORDER BY MIN(`example_PlayerStats`.`lowScore`) DESC LIMIT 5 OFFSET 10";
        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(expectedQueryStr1);
        expectedQueryList.add(expectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(TestQuery.COMPLICATED.getQuery());
    }

    @Test
    public void testNestedMetricQuery() {
        Query query = TestQuery.NESTED_METRIC_QUERY.getQuery();

        String exptectedQueryStr = getExpectedNestedMetricQuery();

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(TestQuery.NESTED_METRIC_QUERY.getQuery());
    }

    @Test
    public void testNestedMetricWithHavingQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_HAVING_QUERY.getQuery();

        String exptectedQueryStr = getExpectedNestedMetricWithHavingQuery();

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(TestQuery.NESTED_METRIC_WITH_HAVING_QUERY.getQuery());
    }

    @Test
    public void testNestedMetricWithWhereQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_WHERE_QUERY.getQuery();

        String exptectedQueryStr = getExpectedNestedMetricWithWhereQuery();

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(TestQuery.NESTED_METRIC_WITH_WHERE_QUERY.getQuery());
    }

    @Test
    public void testNestedMetricWithPaginationQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_PAGINATION_QUERY.getQuery();

        String exptectedQueryStr1 = "SELECT COUNT(*) FROM "
                + "(SELECT `example_PlayerStats_XXX`.`overallRating`, "
                + "`example_PlayerStats_XXX`.`recordedDate` "
                + "FROM (SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                + "`example_PlayerStats`.`overallRating` AS `overallRating`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate_XXX`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') AS `recordedDate` "
                + "FROM `playerStats` AS `example_PlayerStats` "
                + "GROUP BY `example_PlayerStats`.`overallRating`, "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') ) "
                + "AS `example_PlayerStats_XXX` "
                + "GROUP BY `example_PlayerStats_XXX`.`overallRating`, "
                + "`example_PlayerStats_XXX`.`recordedDate` ) AS `pagination_subquery`\n";

        String exptectedQueryStr2 = "SELECT AVG(`example_PlayerStats_XXX`.`highScore`) "
                + "AS `dailyAverageScorePerPeriod`,`example_PlayerStats_XXX`.`overallRating` AS `overallRating`,"
                + "`example_PlayerStats_XXX`.`recordedDate` AS `recordedDate` "
                + "FROM (SELECT MAX(`example_PlayerStats`.`highScore`) AS `highScore`,"
                + "`example_PlayerStats`.`overallRating` AS `overallRating`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd') AS `recordedDate_XXX`,"
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') AS `recordedDate` "
                + "FROM `playerStats` AS `example_PlayerStats` GROUP BY "
                + "`example_PlayerStats`.`overallRating`, "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-dd'), 'yyyy-MM-dd'), "
                + "PARSEDATETIME(FORMATDATETIME(`example_PlayerStats`.`recordedDate`, 'yyyy-MM-01'), 'yyyy-MM-dd') ) "
                + "AS `example_PlayerStats_XXX` GROUP BY "
                + "`example_PlayerStats_XXX`.`overallRating`, "
                + "`example_PlayerStats_XXX`.`recordedDate` "
                + "LIMIT 1 OFFSET 0\n";

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr1);
        expectedQueryList.add(exptectedQueryStr2);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(TestQuery.NESTED_METRIC_WITH_PAGINATION_QUERY.getQuery());
    }

    @Test
    public void testNestedMetricWithSortingQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_SORTING_QUERY.getQuery();

        String exptectedQueryStr = getExpectedNestedMetricWithSortingQuery(false);

        List<String> expectedQueryList = new ArrayList<>();
        expectedQueryList.add(exptectedQueryStr);

        compareQueryLists(expectedQueryList, engine.explain(query));

        testQueryExecution(TestQuery.NESTED_METRIC_WITH_SORTING_QUERY.getQuery());
    }

    @Test
    public void testNestedMetricWithAliasesQuery() {
        Query query = TestQuery.NESTED_METRIC_WITH_ALIASES_QUERY.getQuery();

        String queryStr = engine.explain(query).get(0);
        queryStr = repeatedWhitespacePattern.matcher(queryStr).replaceAll(" ");
        queryStr = queryStr.replaceAll(":[a-zA-Z0-9_]+", ":XXX");
        queryStr = queryStr.replaceAll("PlayerStats_\\d+", "PlayerStats_XXX");
        queryStr = queryStr.replaceAll("PlayerStats_country_\\d+", "PlayerStats_country_XXX");

        String expectedStr = getExpectedNestedMetricWithAliasesSQL(false);
        assertEquals(expectedStr, queryStr);

        testQueryExecution(query);
    }

    @Test
    public void testWhereWithArguments() {
        Query query = TestQuery.WHERE_WITH_ARGUMENTS.getQuery();

        String queryStr = engine.explain(query).get(0);
        queryStr = repeatedWhitespacePattern.matcher(queryStr).replaceAll(" ");

        assertEquals(getExpectedWhereWithArgumentsSQL(), queryStr);

        testQueryExecution(query);
    }

    @Test
    public void testLeftJoin() throws Exception {
        Query query = TestQuery.LEFT_JOIN.getQuery();

        String expectedQueryStr =
                        "SELECT DISTINCT `example_VideoGame_player_XXX`.`name` AS `playerName` FROM `videoGames` AS `example_VideoGame`"
                                        + " LEFT OUTER JOIN `players` AS `example_VideoGame_player_XXX` ON `example_VideoGame`.`player_id`"
                                        + " = `example_VideoGame_player_XXX`.`id`";

        compareQueryLists(expectedQueryStr, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testInnerJoin() throws Exception {
        Query query = TestQuery.INNER_JOIN.getQuery();

        String expectedQueryStr =
                        "SELECT DISTINCT `example_VideoGame_playerInnerJoin_XXX`.`name` AS `playerNameInnerJoin` FROM `videoGames` AS `example_VideoGame`"
                                        + " INNER JOIN `players` AS `example_VideoGame_playerInnerJoin_XXX` ON `example_VideoGame`.`player_id`"
                                        + " = `example_VideoGame_playerInnerJoin_XXX`.`id`";

        compareQueryLists(expectedQueryStr, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testCrossJoin() throws Exception {
        Query query = TestQuery.CROSS_JOIN.getQuery();

        String expectedQueryStr =
                        "SELECT DISTINCT `example_VideoGame_playerCrossJoin_XXX`.`name` AS `playerNameCrossJoin` FROM `videoGames` AS `example_VideoGame`"
                                        + " CROSS JOIN `players` AS `example_VideoGame_playerCrossJoin_XXX`";

        compareQueryLists(expectedQueryStr, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testJoinWithMetrics() throws Exception {
        Query query = TestQuery.METRIC_JOIN.getQuery();

        String expectedQueryStr = "SELECT "
                + "MAX(`example_VideoGame_playerStats_XXX`.`highScore`) / SUM(`example_VideoGame`.`timeSpent`) AS `normalizedHighScore` "
                + "FROM `videoGames` AS `example_VideoGame` "
                + "LEFT OUTER JOIN `playerStats` AS `example_VideoGame_playerStats_XXX` "
                + "ON `example_VideoGame`.`player_id` = `example_VideoGame_playerStats_XXX`.`id`";

        compareQueryLists(expectedQueryStr, engine.explain(query));
        testQueryExecution(query);
    }

    @Test
    public void testPaginationMetricsOnly() throws Exception {
        // pagination query should be empty since there is no dimension projection
        Query query = TestQuery.PAGINATION_METRIC_ONLY.getQuery();
        String expectedQueryStr =
                "SELECT MIN(`example_PlayerStats`.`lowScore`) AS `lowScore` "
                        + "FROM `playerStats` AS `example_PlayerStats` "
                        + "LIMIT 5 OFFSET 10\n";
        compareQueryLists(expectedQueryStr, engine.explain(query));

        testQueryExecution(TestQuery.PAGINATION_METRIC_ONLY.getQuery());
    }
}
