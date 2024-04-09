/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import static com.paiondata.elide.test.graphql.GraphQLDSL.argument;
import static com.paiondata.elide.test.graphql.GraphQLDSL.field;
import static com.paiondata.elide.test.graphql.GraphQLDSL.mutation;
import static com.paiondata.elide.test.graphql.GraphQLDSL.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.ExceptionMappers;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.paiondata.elide.test.graphql.GraphQLDSL;

import example.models.versioned.BookV2;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;

import graphqlEndpointTestModels.Author;
import graphqlEndpointTestModels.Book;
import graphqlEndpointTestModels.DisallowTransfer;
import graphqlEndpointTestModels.Incident;
import graphqlEndpointTestModels.security.CommitChecks;
import graphqlEndpointTestModels.security.UserChecks;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GraphQL endpoint tests tested against the in-memory store.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GraphQLEndpointTest {

    private GraphQLEndpoint endpoint;
    private final SecurityContext user1 = Mockito.mock(SecurityContext.class);
    private final SecurityContext user2 = Mockito.mock(SecurityContext.class);
    private final SecurityContext user3 = Mockito.mock(SecurityContext.class);
    private final AuditLogger audit = Mockito.mock(AuditLogger.class);
    private final UriInfo uriInfo = Mockito.mock(UriInfo.class);
    private final HttpHeaders requestHeaders = Mockito.mock(HttpHeaders.class);
    private final DataFetcherExceptionHandler dataFetcherExceptionHandler = Mockito.spy(new SimpleDataFetcherExceptionHandler());

    private Elide elide;
    private ExceptionMappers exceptionMappers;

    public static class User implements Principal {
        String log = "";
        String name;

        @Override
        public String getName() {
            return name;
        }

        public User withName(String name) {
            this.name = name;
            return this;
        }

        public void appendLog(String stmt) {
            log = log + stmt;
        }

        public String getLog() {
            return log;
        }

        @Override
        public String toString() {
            return getLog();
        }
    }

    @BeforeAll
    public void setup() {
        Mockito.when(user1.getUserPrincipal()).thenReturn(new User().withName("1"));
        Mockito.when(user2.getUserPrincipal()).thenReturn(new User().withName("2"));
        Mockito.when(user3.getUserPrincipal()).thenReturn(new User().withName("3"));
        Mockito.when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/graphql"));
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        Mockito.when(requestHeaders.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
    }

    @BeforeEach
    public void setupTest() throws Exception {
        HashMapDataStore inMemoryStore = new HashMapDataStore(new DefaultClassScanner(),
                Book.class.getPackage());
        Map<String, Class<? extends Check>> checkMappings = new HashMap<>();

        checkMappings.put(UserChecks.IS_USER_1, UserChecks.IsUserId.One.class);
        checkMappings.put(UserChecks.IS_USER_2, UserChecks.IsUserId.Two.class);
        checkMappings.put(CommitChecks.IS_NOT_USER_3, CommitChecks.IsNotUser3.class);

        exceptionMappers = Mockito.mock(ExceptionMappers.class);

        EntityDictionary entityDictionary = EntityDictionary.builder().checks(checkMappings).build();
        elide = spy(
                new Elide(
                    ElideSettings.builder().dataStore(inMemoryStore)
                            .entityDictionary(entityDictionary)
                            .auditLogger(audit)
                                .settings(GraphQLSettings.GraphQLSettingsBuilder.withDefaults(entityDictionary).graphqlExceptionHandler(
                                        new DefaultGraphQLExceptionHandler(new Slf4jExceptionLogger(), exceptionMappers,
                                                new DefaultGraphQLErrorMapper())))
                                .build())

                );

        elide.doScans();
        endpoint = new GraphQLEndpoint(elide, Optional.of(dataFetcherExceptionHandler), Optional.empty());

        DataStoreTransaction tx = inMemoryStore.beginTransaction();

        // Initial data
        Book book1 = new Book();
        Author author1 = new Author();
        Author author2 = new Author();
        DisallowTransfer noShare = new DisallowTransfer();

        book1.setId(1L);
        book1.setTitle("My first book");
        book1.setAuthors(Sets.newHashSet(author1));

        author1.setId(1L);
        author1.setName("Ricky Carmichael");
        author1.setBooks(Sets.newHashSet(book1));
        author1.setBookTitlesAndAwards(
                Stream.of(
                        new AbstractMap.SimpleImmutableEntry<>("Bookz", "Pulitzer Prize"),
                        new AbstractMap.SimpleImmutableEntry<>("Lost in the Data", "PEN/Faulkner Award")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        author2.setId(2L);
        author2.setName("The Silent Author");
        author2.setBookTitlesAndAwards(
                Stream.of(
                        new AbstractMap.SimpleImmutableEntry<>("Working Hard or Hardly Working", "Booker Prize")
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        noShare.setId(1L);

        tx.createObject(book1, null);
        tx.createObject(author1, null);
        tx.createObject(author2, null);
        tx.createObject(noShare, null);

        tx.save(book1, null);
        tx.save(author1, null);
        tx.save(author2, null);
        tx.save(noShare, null);

        tx.commit(null);

        reset(dataFetcherExceptionHandler);
    }

    @Test
    public void testValidFetch() throws JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("name")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String graphQLResponse = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("title", "My first book"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("name", "Ricky Carmichael")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testValidFetchWithVariables() throws JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.query(
                        "myQuery",
                        GraphQLDSL.variableDefinitions(
                                GraphQLDSL.variableDefinition("bookId", "[String]")
                        ),
                        GraphQLDSL.selections(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.arguments(
                                                GraphQLDSL.argument("ids", "$bookId")
                                        ),
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field("title"),
                                                GraphQLDSL.field(
                                                        "authors",
                                                        GraphQLDSL.selection(
                                                                GraphQLDSL.field("name")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String graphQLResponse = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("title", "My first book"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("name", "Ricky Carmichael")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Map<String, String> variables = new HashMap<>();
        variables.put("bookId", "1");
        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest, variables));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testCanReadRestrictedFieldWithAppropriateAccess() throws JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selection(
                                        GraphQLDSL.field("user1SecretField")
                                )
                        )
                )
        ).toQuery();

        String graphQLResponse = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selection(
                                        GraphQLDSL.field("user1SecretField", "this is a secret for user 1 only1")
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    void testCannotReadRestrictedField() throws JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selection(
                                        GraphQLDSL.field("user1SecretField")
                                )
                        )
                )
        ).toQuery();

        //Empty response because the collection is skipped because of failed user check.
        String expected = "{\"data\":{\"book\":{\"edges\":[]}}}";

        Response response = endpoint.post("", uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }


    @Test
    void testPartialResponse() throws IOException, JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selection(
                                        GraphQLDSL.field("user1SecretField")
                                )
                        ),
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post("", uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
        verify(exceptionMappers).toErrorResponse(any(), any());
    }

    @Test
    void testCrypticErrorOnUpsert() throws IOException, JSONException {
        Incident incident = new Incident();

        String graphQLRequest = GraphQLDSL.document(
           GraphQLDSL.mutation(
               GraphQLDSL.selection(
                   GraphQLDSL.field(
                       "incidents",
                       GraphQLDSL.arguments(
                           GraphQLDSL.argument("op", "UPSERT"),
                           GraphQLDSL.argument("data", incident)
                       ),
                       GraphQLDSL.selections(
                           GraphQLDSL.field("id"),
                           GraphQLDSL.field("name")
                       )
                   )
               )
           )
        ).toQuery();

        Response response = endpoint.post("", uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        JsonNode node = extract200Response(response);
        Iterator<JsonNode> errors = node.get("errors").elements();
        assertTrue(errors.hasNext());
        assertTrue(errors.next().get("message").asText().contains("No id provided, cannot persist incidents"));
        verify(exceptionMappers).toErrorResponse(any(), any());
    }

    @Test
    void testFailedMutationAndRead() throws IOException, JSONException {
        Author author = new Author();
        author.setId(2L);

        Book book = new Book();
        book.setId(1);
        book.setTitle("my new book!");
        book.setAuthors(Sets.newHashSet(author));

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.arguments(
                                        GraphQLDSL.argument("op", "UPSERT"),
                                        GraphQLDSL.argument("data", book)
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title")
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post("", uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
        verify(dataFetcherExceptionHandler).handleException(any());

        graphQLRequest = GraphQLDSL.document(
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

        String expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                               GraphQLDSL.selections(
                                       GraphQLDSL.field("id", "1"),
                                       GraphQLDSL.field("title", "My first book")
                               )
                        )
                )
        ).toResponse();

        response = endpoint.post("", uriInfo, requestHeaders, user2, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testNonShareable() throws IOException, JSONException {
        DisallowTransfer noShare = new DisallowTransfer();
        noShare.setId(1L);

        Author author = new Author();
        author.setId(123L);
        author.setName("my new author");
        author.setNoShare(noShare);

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.mutation(
                        GraphQLDSL.selection(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field(
                                                        "authors",
                                                        GraphQLDSL.arguments(
                                                                GraphQLDSL.argument("op", "UPSERT"),
                                                                GraphQLDSL.argument("data", author)
                                                        ),
                                                        GraphQLDSL.selections(
                                                                GraphQLDSL.field("id"),
                                                                GraphQLDSL.field("name"),
                                                                GraphQLDSL.field(
                                                                        "noShare",
                                                                        GraphQLDSL.selection(
                                                                                GraphQLDSL.field("id")
                                                                        )
                                                                )
                                                        )
                                                        )
                                        )
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));

        assertHasErrors(response);

        graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("id"),
                                                        GraphQLDSL.field("name"),
                                                        GraphQLDSL.field(
                                                                "noShare",
                                                                GraphQLDSL.selection(
                                                                        GraphQLDSL.field("id")
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("id", "1"),
                                                        GraphQLDSL.field("name", "Ricky Carmichael"),
                                                        GraphQLDSL.field("noShare", "", false)
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testLifeCycleHooks () throws Exception {
        /* Separate user 1 so it doesn't interfere */
        SecurityContext user = Mockito.mock(SecurityContext.class);
        User principal = new User().withName("1");
        Mockito.when(user.getUserPrincipal()).thenReturn(principal);

        Book book = new Book();
        book.setId(1);
        book.setTitle("my new book!");

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.mutation(
                        GraphQLDSL.selection(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.arguments(
                                                GraphQLDSL.argument("op", "UPSERT"),
                                                GraphQLDSL.argument("data", book)
                                        ),
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("title", "my new book!")
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post("", uriInfo, requestHeaders, user, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);

        String expectedLog = "On Title Update Pre Security\nOn Title Update Pre Commit\nOn Title Update Post Commit\n";

        assertEquals(principal.getLog(), expectedLog);
    }

    @Test
    void testAuditLogging() throws IOException {
        Mockito.reset(audit);

        Book book = new Book();
        book.setTitle("my new book!");

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.mutation(
                        GraphQLDSL.selection(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.arguments(
                                                GraphQLDSL.argument("op", "UPSERT"),
                                                GraphQLDSL.argument("data", book)
                                        ),
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));

        verify(audit, Mockito.times(1)).log(Mockito.any());
        verify(audit, Mockito.times(1)).commit();
        verify(audit, Mockito.times(1)).clear();
    }

    @Test
    void testSuccessfulMutation() throws JSONException {
        Author author = new Author();
        author.setId(2L);

        Book book = new Book();
        book.setId(123);
        book.setTitle("my new book!");
        book.setAuthors(Sets.newHashSet(author));

        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.mutation(
                        GraphQLDSL.selection(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.arguments(
                                                GraphQLDSL.argument("op", "UPSERT"),
                                                GraphQLDSL.argument("data", book)
                                        ),
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field("title"),
                                                GraphQLDSL.field("user1SecretField")
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "2"),
                                        GraphQLDSL.field("title", "my new book!"),
                                        GraphQLDSL.field("user1SecretField", "this is a secret for user 1 only1")
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);

        graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("id"),
                                                        GraphQLDSL.field("name")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("title", "My first book"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("id", "1"),
                                                        GraphQLDSL.field("name", "Ricky Carmichael")
                                                )
                                        )
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "2"),
                                        GraphQLDSL.field("title", "my new book!"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("id", "2"),
                                                        GraphQLDSL.field("name", "The Silent Author")
                                                )
                                        )
                                )

                        )
                )
        ).toResponse();

        response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testFailedCommitCheck() throws IOException {
        Book book = new Book();
        book.setId(1);
        book.setTitle("update title");

        // NOTE: User 3 cannot update books.
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.mutation(
                        GraphQLDSL.selection(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.arguments(
                                                GraphQLDSL.argument("op", "UPSERT"),
                                                GraphQLDSL.argument("data", book)
                                        ),
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field("title")
                                        )
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post("", uriInfo, requestHeaders, user3, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
        verify(exceptionMappers).toErrorResponse(any(), any());
    }

    @Test
    void testQueryAMap() throws JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.mutation(
                        GraphQLDSL.selection(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field(
                                                        "authors",
                                                        GraphQLDSL.selection(
                                                                GraphQLDSL.field("bookTitlesAndAwards {key value}")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        Map<String, String> first = new HashMap<>();
        first.put("key", "Lost in the Data");
        first.put("value", "PEN/Faulkner Award");

        Map<String, String> second = new HashMap<>();
        second.put("key", "Bookz");
        second.put("value", "Pulitzer Prize");

        String expected = GraphQLDSL.document(
                GraphQLDSL.selection(
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field(
                                                                "bookTitlesAndAwards",
                                                                GraphQLDSL.toJson(Arrays.asList(first, second)),
                                                                GraphQLDSL.UNQUOTED_VALUE
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, expected);
    }

    @Test
    void testQueryAMapWithBadFields() throws IOException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.mutation(
                        GraphQLDSL.selection(
                                GraphQLDSL.field(
                                        "book",
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field(
                                                        "authors",
                                                         GraphQLDSL.selection(
                                                                GraphQLDSL.field("bookTitlesAndAwards {key value Bookz}")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assertHasErrors(response);
        verify(exceptionMappers).toErrorResponse(any(), any());
    }


    @Test
    public void testMultipleRoot() throws JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "author",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("name"),
                                        GraphQLDSL.field(
                                                "books",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("title")
                                                )
                                        )
                                )
                        ),
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("title"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("name")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String graphQLResponse = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "author",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("name", "Ricky Carmichael"),
                                        GraphQLDSL.field(
                                                "books",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("title", "My first book")
                                                )
                                        )
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "2"),
                                        GraphQLDSL.field("name", "The Silent Author"),
                                        GraphQLDSL.field(
                                                "books", "", false
                                        )
                                )
                        ),
                        GraphQLDSL.field(
                                "book",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("title", "My first book"),
                                        GraphQLDSL.field(
                                                "authors",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("name", "Ricky Carmichael")
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();


        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    public void testMultipleQueryWithAlias() throws JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "AuthorBook",
                                "author",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field(
                                                "books",
                                                GraphQLDSL.selection(
                                                        GraphQLDSL.field("title")
                                                )
                                        )
                                )
                        ),
                        GraphQLDSL.field(
                                "AuthorName",
                                "author",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id"),
                                        GraphQLDSL.field("name")
                                )
                        )
                )
        ).toQuery();
        String graphQLResponse = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "AuthorBook",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field(
                                                "books",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("title", "My first book")
                                                )
                                        )
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "2"),
                                        GraphQLDSL.field(
                                                "books", "", false
                                        )
                                )
                        ),
                        GraphQLDSL.field(
                                "AuthorName",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("name", "Ricky Carmichael")
                                ),
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "2"),
                                        GraphQLDSL.field("name", "The Silent Author")
                                )
                        )
                )
        ).toResponse();


        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    public void testMultipleQueryWithAliasAndArguments() throws JSONException {
        String graphQLRequest = GraphQLDSL.document(
                GraphQLDSL.query(
                        "myQuery",
                        GraphQLDSL.variableDefinitions(
                                GraphQLDSL.variableDefinition("author1", "[String]"),
                                GraphQLDSL.variableDefinition("author2", "[String]")
                        ),
                        GraphQLDSL.selections(
                                GraphQLDSL.field(
                                        "Author_1",
                                        "author",
                                        GraphQLDSL.arguments(
                                                GraphQLDSL.argument("ids", "$author1")
                                        ),
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field("name"),
                                                GraphQLDSL.field(
                                                        "books",
                                                        GraphQLDSL.selection(
                                                                GraphQLDSL.field("title")
                                                        )
                                                )
                                        )
                                ),
                                GraphQLDSL.field(
                                        "Author_2",
                                        "author",
                                        GraphQLDSL.arguments(
                                                GraphQLDSL.argument("ids", "$author2")
                                        ),
                                        GraphQLDSL.selections(
                                                GraphQLDSL.field("id"),
                                                GraphQLDSL.field("name"),
                                                GraphQLDSL.field(
                                                        "books",
                                                        GraphQLDSL.selection(
                                                                GraphQLDSL.field("title")
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();
        String graphQLResponse = GraphQLDSL.document(
                GraphQLDSL.selections(
                        GraphQLDSL.field(
                                "Author_1",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "1"),
                                        GraphQLDSL.field("name", "Ricky Carmichael"),
                                        GraphQLDSL.field(
                                                "books",
                                                GraphQLDSL.selections(
                                                        GraphQLDSL.field("title", "My first book")
                                                )
                                        )
                                )
                        ),
                        GraphQLDSL.field(
                                "Author_2",
                                GraphQLDSL.selections(
                                        GraphQLDSL.field("id", "2"),
                                        GraphQLDSL.field("name", "The Silent Author"),
                                        GraphQLDSL.field(
                                                "books", "", false
                                        )
                                )
                        )
                )
        ).toResponse();


        Map<String, String> variables = new HashMap<>();
        variables.put("author1", "1");
        variables.put("author2", "2");

        Response response = endpoint.post("", uriInfo, requestHeaders, user1, graphQLRequestToJSON(graphQLRequest, variables));
        assert200EqualBody(response, graphQLResponse);
    }

    @Test
    public void testInvalidApiVersion() throws IOException {
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        entityDictionary.bindEntity(BookV2.class);
        Elide elide = new Elide(
                ElideSettings.builder().entityDictionary(entityDictionary).settings(GraphQLSettings.builder()).build());
        GraphQLEndpoint graphqlEndpoint = new GraphQLEndpoint(elide, Optional.empty(), Optional.empty());
        Response response = graphqlEndpoint.post("/v1", uriInfo, requestHeaders, user1, null);
        Object entity = response.getEntity();
        assertEquals(400, response.getStatus());
        assertInstanceOf(String.class, entity);
        if (entity instanceof String value) {
            assertTrue(value.contains("Invalid API Version"));
        }
    }

    private static String graphQLRequestToJSON(String request) {
        return graphQLRequestToJSON(request, new HashMap<>());
    }

    private static String graphQLRequestToJSON(String request, Map<String, String> variables) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = JsonNodeFactory.instance.objectNode();

        ((ObjectNode) node).put("query", request);
        ((ObjectNode) node).set("variables", variables == null ? null : mapper.valueToTree(variables));
        return node.toString();
    }

    private static JsonNode extract200Response(Response response) throws IOException {
        return new ObjectMapper().readTree(extract200ResponseString(response));
    }

    private static String extract200ResponseString(Response response) {
        assertEquals(200, response.getStatus());
        return (String) response.getEntity();
    }

    private static void assert200EqualBody(Response response, String expected) throws JSONException {
        String actual = extract200ResponseString(response);
        JSONAssert.assertEquals(expected, actual, true);
    }

    private static void assert200DataEqual(Response response, String expected) throws IOException, JSONException {
        JsonNode actualNode = extract200Response(response);

        Iterator<Map.Entry<String, JsonNode>> iterator = actualNode.fields();

        // get json node that has "data" key
        String actual = null;
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> next = iterator.next();
            if (next.getKey() == "data") {
                actual = new ObjectMapper().writeValueAsString(next);
            }
        }

        JSONAssert.assertEquals(expected, actual, true);
    }

    private static void assertHasErrors(Response response) throws IOException {
        JsonNode node = extract200Response(response);
        assertTrue(node.get("errors").elements().hasNext());
    }
}
