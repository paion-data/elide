/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jms;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.paiondata.elide.datastores.jms.TestBinder.EMBEDDED_JMS_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.graphql.subscriptions.hooks.TopicType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import example.Author;
import example.Book;
import example.Chat;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;

import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JMSDataStoreTest {

    protected ConnectionFactory connectionFactory;
    protected EntityDictionary dictionary;
    protected JMSDataStore store;
    protected EmbeddedActiveMQ embedded;

    @BeforeAll
    public void init() throws Exception {
        embedded = new EmbeddedActiveMQ();
        Configuration configuration = new ConfigurationImpl();
        configuration.addAcceptorConfiguration("default", EMBEDDED_JMS_URL);
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.setJournalType(JournalType.NIO);

        embedded.setConfiguration(configuration);
        embedded.start();

        connectionFactory = new ActiveMQConnectionFactory(EMBEDDED_JMS_URL);
        dictionary = EntityDictionary.builder().build();

        store = new JMSDataStore(Sets.newHashSet(ClassType.of(Book.class), ClassType.of(Author.class),
                ClassType.of(Chat.class)),
                connectionFactory, dictionary, new ObjectMapper(), Duration.ofMillis(2500L));
        store.populateEntityDictionary(dictionary);
    }

    @AfterAll
    public void shutdown() throws Exception {
        embedded.stop();
    }

    @Test
    public void testModelLabels() throws Exception {
        assertTrue(store.models.get(ClassType.of(Book.class)));
        assertTrue(store.models.get(ClassType.of(Author.class)));
        assertFalse(store.models.get(ClassType.of(Chat.class)));
    }

    @Test
    public void testLoadObjects() throws Exception {
        Author author1 = new Author();
        author1.setId(1);
        author1.setName("Jon Doe");

        Book book1 = new Book();
        book1.setTitle("Enders Game");
        book1.setId(1);
        book1.setAuthors(Sets.newHashSet(author1));

        Book book2 = new Book();
        book2.setTitle("Grapes of Wrath");
        book2.setId(2);

        try (DataStoreTransaction tx = store.beginReadTransaction()) {

            Route route = Route.builder().baseUrl("/json").path("/").apiVersion(NO_VERSION).build();
            ElideSettings elideSettings = ElideSettings.builder().dataStore(store).entityDictionary(dictionary).build();
            RequestScope scope = RequestScope.builder().route(route).dataStoreTransaction(tx)
                    .requestId(UUID.randomUUID()).elideSettings(elideSettings).build();

            Iterable<Book> books = tx.loadObjects(
                    EntityProjection.builder()
                            .argument(Argument.builder()
                                    .name("topic")
                                    .value(TopicType.ADDED)
                                    .build())
                            .type(Book.class).build(),
                    scope
            );

            JMSContext context = connectionFactory.createContext();
            Destination destination = context.createTopic("bookAdded");

            JMSProducer producer = context.createProducer();
            ObjectMapper mapper = new ObjectMapper();
            producer.send(destination, mapper.writeValueAsString(book1));
            producer.send(destination, mapper.writeValueAsString(book2));

            Iterator<Book> booksIterator = books.iterator();

            assertTrue(booksIterator.hasNext());
            Book receivedBook = booksIterator.next();
            assertEquals("Enders Game", receivedBook.getTitle());
            assertEquals(1, receivedBook.getId());

            Set<Author> receivedAuthors = Sets.newHashSet((Iterable) tx.getToManyRelation(tx, receivedBook,
                    Relationship.builder()
                            .name("authors")
                            .projection(EntityProjection.builder()
                                    .type(Author.class)
                                    .build())
                            .build(), scope));

            assertTrue(receivedAuthors.contains(author1));

            assertTrue(booksIterator.hasNext());
            receivedBook = booksIterator.next();
            assertEquals("Grapes of Wrath", receivedBook.getTitle());
            assertEquals(2, receivedBook.getId());

            assertFalse(booksIterator.hasNext());
        }
    }
}
