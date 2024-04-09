/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;
import com.paiondata.elide.test.jsonapi.elements.PatchOperationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

/**
 * Tests for pagination.
 */
class PaginateIT extends IntegrationTest {

    private String asimovId = null;
    private String hemingwayId = null;
    private String orsonCardId = null;
    private String parentId = null;

    @BeforeEach
    void setup() {
        createFamilyEntities();
        createBookEntities();
        createPaginationEntities();
    }

    private void createPaginationEntities() {
        BiConsumer<String, Integer> createEntities = (type, numberOfEntities) -> {
            IntStream.range(0, numberOfEntities).forEach(value -> given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                    JsonApiDSL.datum(
                        JsonApiDSL.resource(
                            JsonApiDSL.type(type),
                            JsonApiDSL.attributes(
                                JsonApiDSL.attr("name", "A name")
                            )
                        )
                    ).toJSON()
                ).post("/" + type)
                .then()
                .statusCode(CREATED_201));
            get("/" + type).path("data.id");
        };

        createEntities.accept("entityWithoutPaginate", 20);
        createEntities.accept("entityWithPaginateCountableFalse", 5);
        createEntities.accept("entityWithPaginateDefaultLimit", 5);
        createEntities.accept("entityWithPaginateMaxLimit", 30);
    }

    private void createBookEntities() {
        String tempAuthorId1 = "12345678-1234-1234-1234-1234567890ab";
        String tempAuthorId2 = "12345679-1234-1234-1234-1234567890ab";
        String tempAuthorId3 = "12345681-1234-1234-1234-1234567890ab";

        String tempBookId1 = "12345678-1234-1234-1234-1234567890ac";
        String tempBookId2 = "12345678-1234-1234-1234-1234567890ad";
        String tempBookId3 = "12345679-1234-1234-1234-1234567890ac";
        String tempBookId4 = "23451234-1234-1234-1234-1234567890ac";
        String tempBookId5 = "12345680-1234-1234-1234-1234567890ac";
        String tempBookId6 = "12345680-1234-1234-1234-1234567890ad";
        String tempBookId7 = "12345681-1234-1234-1234-1234567890ac";
        String tempBookId8 = "12345681-1234-1234-1234-1234567890ad";

        String tempPubId = "12345678-1234-1234-1234-1234567890ae";

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                JsonApiDSL.patchSet(
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/author", JsonApiDSL.resource(
                        JsonApiDSL.type("author"),
                        JsonApiDSL.id(tempAuthorId1),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("name", "Ernest Hemingway")
                        ),
                        JsonApiDSL.relationships(
                            JsonApiDSL.relation("books",
                                JsonApiDSL.linkage(JsonApiDSL.type("book"), JsonApiDSL.id(tempBookId1)),
                                JsonApiDSL.linkage(JsonApiDSL.type("book"), JsonApiDSL.id(tempBookId2))
                            )
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book/", JsonApiDSL.resource(
                        JsonApiDSL.type("book"),
                        JsonApiDSL.id(tempBookId1),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("title", "The Old Man and the Sea"),
                            JsonApiDSL.attr("genre", "Literary Fiction"),
                            JsonApiDSL.attr("language", "English")
                        ),
                        JsonApiDSL.relationships(
                            JsonApiDSL.relation("publisher",
                                JsonApiDSL.linkage(JsonApiDSL.type("publisher"), JsonApiDSL.id(tempPubId))

                            )
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book/", JsonApiDSL.resource(
                        JsonApiDSL.type("book"),
                        JsonApiDSL.id(tempBookId2),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("title", "For Whom the Bell Tolls"),
                            JsonApiDSL.attr("genre", "Literary Fiction"),
                            JsonApiDSL.attr("language", "English")
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book/" + tempBookId1 + "/publisher", JsonApiDSL.resource(
                        JsonApiDSL.type("publisher"),
                        JsonApiDSL.id(tempPubId),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("name", "Default publisher")
                        )
                    ))
                ).toJSON()
            )
            .patch("/")
            .then()
            .statusCode(OK_200);

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                JsonApiDSL.patchSet(
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/author", JsonApiDSL.resource(
                        JsonApiDSL.type("author"),
                        JsonApiDSL.id(tempAuthorId2),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("name", "Orson Scott Card")
                        ),
                        JsonApiDSL.relationships(
                            JsonApiDSL.relation("books",
                                JsonApiDSL.linkage(JsonApiDSL.type("book"), JsonApiDSL.id(tempBookId3)),
                                JsonApiDSL.linkage(JsonApiDSL.type("book"), JsonApiDSL.id(tempBookId4))
                            )
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book", JsonApiDSL.resource(
                        JsonApiDSL.type("book"),
                        JsonApiDSL.id(tempBookId3),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("title", "Enders Game"),
                            JsonApiDSL.attr("genre", "Science Fiction"),
                            JsonApiDSL.attr("language", "English"),
                            JsonApiDSL.attr("publishDate", 1454638927412L)
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book", JsonApiDSL.resource(
                        JsonApiDSL.type("book"),
                        JsonApiDSL.id(tempBookId4),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("title", "Enders Shadow"),
                            JsonApiDSL.attr("genre", "Science Fiction"),
                            JsonApiDSL.attr("language", "English"),
                            JsonApiDSL.attr("publishDate", 1464638927412L)
                        )
                    ))
                )
            )
            .patch("/")
            .then()
            .statusCode(OK_200);

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                JsonApiDSL.patchSet(
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/author", JsonApiDSL.resource(
                        JsonApiDSL.type("author"),
                        JsonApiDSL.id(tempAuthorId3),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("name", "Isaac Asimov")
                        ),
                        JsonApiDSL.relationships(
                            JsonApiDSL.relation("books",
                                JsonApiDSL.linkage(JsonApiDSL.type("book"), JsonApiDSL.id(tempBookId5)),
                                JsonApiDSL.linkage(JsonApiDSL.type("book"), JsonApiDSL.id(tempBookId6))
                            )
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book", JsonApiDSL.resource(
                        JsonApiDSL.type("book"),
                        JsonApiDSL.id(tempBookId5),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("title", "Foundation"),
                            JsonApiDSL.attr("genre", "Science Fiction"),
                            JsonApiDSL.attr("language", "English")
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book", JsonApiDSL.resource(
                        JsonApiDSL.type("book"),
                        JsonApiDSL.id(tempBookId6),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("title", "The Roman Republic"),
                            //genre null
                            JsonApiDSL.attr("language", "English")
                        )
                    ))
                )
            )
            .patch("/")
            .then()
            .statusCode(OK_200);

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                JsonApiDSL.patchSet(
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/author", JsonApiDSL.resource(
                        JsonApiDSL.type("author"),
                        JsonApiDSL.id(tempAuthorId3),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("name", "Null Ned")
                        ),
                        JsonApiDSL.relationships(
                            JsonApiDSL.relation("books",
                                JsonApiDSL.linkage(JsonApiDSL.type("book"), JsonApiDSL.id(tempBookId7)),
                                JsonApiDSL.linkage(JsonApiDSL.type("book"), JsonApiDSL.id(tempBookId8))
                            )
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book", JsonApiDSL.resource(
                        JsonApiDSL.type("book"),
                        JsonApiDSL.id(tempBookId7),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("title", "Life with Null Ned"),
                            JsonApiDSL.attr("language", "English")
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/book", JsonApiDSL.resource(
                        JsonApiDSL.type("book"),
                        JsonApiDSL.id(tempBookId8),
                        JsonApiDSL.attributes(
                            JsonApiDSL.attr("title", "Life with Null Ned 2"),
                            JsonApiDSL.attr("genre", "Not Null"),
                            JsonApiDSL.attr("language", "English")
                        )
                    ))
                ).toJSON()
            )
            .patch("/")
            .then()
            .statusCode(OK_200);

        Response authors = get("/author").then().extract().response();
        UnaryOperator<String> findAuthorId = name ->
            authors.path("data.find { it.attributes.name=='" + name + "' }.id");

        asimovId = findAuthorId.apply("Isaac Asimov");
        orsonCardId = findAuthorId.apply("Orson Scott Card");
        hemingwayId = findAuthorId.apply("Ernest Hemingway");
    }

    private void createFamilyEntities() {
        String tempParentId = "12345678-1234-1234-1234-1234567890ab";
        String tempChildId1 = "12345678-1234-1234-1234-1234567890ac";
        String tempChildId2 = "12345678-1234-1234-1234-1234567890ad";
        String tempSpouseId = "12345678-1234-1234-1234-1234567890af";

        given()
            .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
            .accept(JsonApi.JsonPatch.MEDIA_TYPE)
            .body(
                JsonApiDSL.patchSet(
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/parent", JsonApiDSL.resource(
                        JsonApiDSL.type("parent"),
                        JsonApiDSL.id(tempParentId),
                        JsonApiDSL.relationships(
                            JsonApiDSL.relation("children",
                                JsonApiDSL.linkage(JsonApiDSL.type("child"), JsonApiDSL.id(tempChildId1)),
                                JsonApiDSL.linkage(JsonApiDSL.type("child"), JsonApiDSL.id(tempChildId2))
                            ),
                            JsonApiDSL.relation("spouses",
                                JsonApiDSL.linkage(JsonApiDSL.type("parent"), JsonApiDSL.id(tempSpouseId))
                            )
                        )
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/parent/" + tempParentId + "/children", JsonApiDSL.resource(
                        JsonApiDSL.type("child"),
                        JsonApiDSL.id(tempChildId1)
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/parent/" + tempParentId + "/children", JsonApiDSL.resource(
                        JsonApiDSL.type("child"),
                        JsonApiDSL.id(tempChildId2)
                    )),
                    JsonApiDSL.patchOperation(PatchOperationType.add, "/parent", JsonApiDSL.resource(
                        JsonApiDSL.type("parent"),
                        JsonApiDSL.id(tempSpouseId)
                    ))
                ).toJSON()
            )
            .patch("/")
            .then()
            .statusCode(OK_200);

        Response parents = get("/parent").then().extract().response();
        parentId = parents.path("data[0].id");
    }

    @Test
    void testNoFilterSortDescPaginationFirstPage() {
        String url = "/book?sort=-title&page[size]=3";
        when()
            .get(url)
        .then()
            .body("data.attributes.title",
                contains("The Roman Republic", "The Old Man and the Sea", "Life with Null Ned 2"));
    }

    @Test
    void testPaginationOnSubRecords() {
        String url = "/author/" + orsonCardId + "/books?sort=-title&page[size]=1";
        when()
            .get(url)
        .then()
            .body("data.attributes.title", contains("Enders Shadow"));
    }

    @Test
    void testNoFilterSortDescPagination() {
        String url = "/book?sort=-title&page[number]=2&page[size]=3";
        when()
            .get(url)
        .then()
            .body("data.attributes.title", contains("Life with Null Ned", "Foundation", "For Whom the Bell Tolls"));
    }

    @Test
    void testNoFilterMultiSortPagination() {
        //select * from book order by title desc, genre asc;
        String url = "/book?sort=-title,genre&page[size]=3";
        when()
            .get(url)
        .then()
            .body("data.attributes.title",
                contains("The Roman Republic", "The Old Man and the Sea", "Life with Null Ned 2"),
                "data.attributes.genre", contains(null, "Literary Fiction", "Not Null")
            );
        //"The Roman Republic has a null genre and should be should be first.
    }

    @Test
    void testPublishDateLessThanFilter() {
        String url = "/book?filter[book.publishDate][lt]=1454638927411&page[size]=2";
        Response response = get(url).then().extract().response();

        List<?> allBooks = response.path("data.attributes");
        assertEquals(2, allBooks.size());

        List<?> filteredBooks = response.path("data.findAll { it.attributes.publishDate < 1454638927411L }");
        assertEquals(allBooks.size(), filteredBooks.size());
    }

    @Test
    void testPageAndSortOnSubRecords() {

        String url = "/author/" + orsonCardId + "/books?sort=-title,publishDate&page[size]=1";
        when()
            .get(url)
        .then()
            .body("data", hasSize(1),
                "data[0].attributes.publishDate", equalTo(1464638927412L),
                "data[0].relationships.authors.data.id", contains(orsonCardId)
            );
    }

    @Test
    void testPageAndSortOnSubRecordsPageTwo() {
        String url = "/author/" + orsonCardId + "/books?sort=-title&page[number]=2&page[size]=1";
        when()
            .get(url)
        .then()
            .body("data", hasSize(1),
                "data[0].attributes.title", equalTo("Enders Game"),
                "data[0].relationships.authors.data.id", contains(orsonCardId),
                "data[0].relationships.authors.data.id", contains(orsonCardId)
            );
    }

    @Test
    void testPageAndSortShouldFailOnBadSortFields() {
        String url = "/author/" + orsonCardId + "/books?sort=-title,publishDate,onion&page[size]=1";
        when()
            .get(url)
        .then()
            .body("errors", hasSize(1),
                "errors[0].detail",
                equalTo("Invalid value: book does not contain the field onion"))
            .statusCode(BAD_REQUEST_400);

    }

    @Test
    void testBasicPageBasedPagination() {
        String url = "/book?page[number]=2&page[size]=2";
        when()
            .get(url)
        .then()
            .body("data", hasSize(2));
    }

    @Test
    void testBasicOffsetBasedPagination() {
        String url = "/book?page[offset]=3&page[limit]=2";
        when()
            .get(url)
        .then()
            .body("data", hasSize(2));
    }

    @Test
    void testPaginationOffsetOnly() {
        String url = "/book?page[offset]=3";
        when()
            .get(url)
        .then()
            .body("data", hasSize(5));
    }

    @Test
    void testPaginationSizeOnly() {
        String url = "/book?page[size]=2";
        when()
            .get(url)
        .then()
            .body("data", hasSize(2));
    }

    @Test()
    void testPaginationOffsetWithSorting() {
        String url = "/book?sort=title&page[offset]=3";
        when()
            .get(url)
            .then()
            .body("data", hasSize(5),
                "data[0].attributes.title", equalTo("Foundation")
            );
    }

    @Test
    void testPaginateInvalidParameter() {
        String url = "/entityWithoutPaginate?page[bad]=2&page[totals]";
        when()
            .get(url)
        .then()
            .body("errors[0].detail", containsString("Invalid Pagination Parameter"))
            .statusCode(BAD_REQUEST_400);
    }

    @Test
    void testPaginateAnnotationTotals() {
        String url = "/entityWithoutPaginate?page[size]=2&page[totals]";
        when()
            .get(url)
        .then()
            .body("data", hasSize(2),
                "meta.page.totalRecords", equalTo(20),
                "meta.page.totalPages", equalTo(10)
            );
    }

    @Test
    void testPaginateAnnotationTotalsWithFilter() {
        String url = "/entityWithoutPaginate?page[size]=2&page[totals]&filter[entityWithoutPaginate.id][le]=10";
        when()
            .get(url)
        .then()
            .body("data", hasSize(2),
                "meta.page.totalRecords", equalTo(10),
                "meta.page.totalPages", equalTo(5)
            );
    }

    @Test
    @Tag("skipInMemory")
    void testPaginateAnnotationTotalsWithToManyJoinFilter() {
        /* Test RSQL Global */
        String url = "/author?page[totals]&filter=books.title=in=('The Roman Republic','Foundation','Life With Null Ned')";
        when()
            .get(url)
        .then()
            .body("data", hasSize(2),
                "data.attributes.name", contains("Isaac Asimov", "Null Ned"),
                "meta.page.totalRecords", equalTo(2)
            );
    }

    @Test
    void testRelationshipPaginateAnnotationTotals() {
        String url = "/author/" + asimovId + "/books?page[size]=1&page[totals]";
        when()
            .get(url)
        .then()
            .body("data", hasSize(1),
                "meta.page.totalRecords", equalTo(2),
                "meta.page.totalPages", equalTo(2)
            );
    }

    @Test
    void testRelationshipPaginateAnnotationTotalsWithFilter() {
        String url = "/author/" + asimovId + "/books?page[size]=1&page[totals]&filter[book.title][infixi]=FounDation";
        when()
            .get(url)
        .then()
            .body("data", hasSize(1),
                "meta.page.totalRecords", equalTo(1),
                "meta.page.totalPages", equalTo(1)
            );
    }

    @Test
    void testPageTotalsForSameTypedRelationship() {
        String url = "/parent/" + parentId + "/spouses?page[totals]";
        when()
            .get(url)
        .then()
            .body("data", hasSize(1),
                "meta.page.totalRecords", equalTo(1),
                "meta.page.totalPages.", equalTo(1)
            );
    }

    @Test
    void testRelationshipPaginateAnnotationTotalsWithNestedFilter() {
        String url = "/author/" + hemingwayId + "/books?filter[book.publisher.name]=Default publisher&page[totals]";
        when()
            .get(url)
        .then()
            .body("data", hasSize(1),
                "meta.page.totalRecords", equalTo(1),
                "meta.page.totalPages.", equalTo(1)
            );
    }

    @Test
    void testPaginateAnnotationPreventTotals() {
        String url = "/entityWithPaginateCountableFalse?page[size]=3&page[totals]";
        when()
            .get(url)
        .then()
            .body("data", hasSize(3),
                "meta.page.totalRecords", nullValue(),
                "meta.page.totalPages.", nullValue()
            );
    }

    @Test
    void testPaginateAnnotationDefaultLimit() {
        String url = "/entityWithPaginateDefaultLimit?page[number]=1";
        when()
            .get(url)
        .then()
            .body("data", hasSize(5),
                "meta.page.number", equalTo(1),
                "meta.page.limit", equalTo(5)
            );
    }

    @Test
    void testPaginateAnnotationMaxLimit() {
        String url = "/entityWithPaginateMaxLimit?page[limit]=100";
        when()
            .get(url)
        .then()
            .body("errors", hasSize(1),
                "errors[0].detail", containsString("Invalid value: Pagination limit must be less than or equal to 10"))
            .statusCode(BAD_REQUEST_400);
    }

    @Test
    void testPaginationNotPossibleAtRoot() {
        String url = "/child?page[size]=1";
        when()
            .get(url)
        .then()
            .body("errors", hasSize(1),
                "errors[0].detail", containsString("Cannot paginate child")
            ).statusCode(BAD_REQUEST_400);
    }

    @Test
    void testPaginationNotPossibleAtRelationship() {
        String url = "/parent/" + parentId + "/children?page[size]=1";
        when()
            .get(url)
        .then()
            .body("errors", hasSize(1),
                "errors[0].detail", containsString("Cannot paginate child")
            );
    }

    @Test
    void testPaginationTotalsOfEmptyCollection() {
        /* Test RSQL Global */
        String url = "/author?page[totals]&filter=books.title=in=('Does Not Exist')";
        when()
                .get(url)
                .then()
                .body("data", hasSize(0),
                        "meta.page.totalRecords", equalTo(0)
                );
    }
}
