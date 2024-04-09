/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpa.porting;

import com.paiondata.elide.datastores.jpql.porting.Query;
import com.paiondata.elide.datastores.jpql.porting.QueryLogger;
import com.paiondata.elide.datastores.jpql.porting.Session;

import com.paiondata.elide.datastores.jpa.JpaDataStore;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps a JPA EntityManager allowing most data store logic
 * to not directly depend on a specific version of JPA.
 */
@Slf4j
public class EntityManagerWrapper implements Session {
    private EntityManager entityManager;
    private QueryLogger logger;

    public EntityManagerWrapper(EntityManager entityManager) {
        this(entityManager, JpaDataStore.DEFAULT_LOGGER);
    }

    public EntityManagerWrapper(EntityManager entityManager, QueryLogger logger) {
        this.entityManager = entityManager;
        this.logger = logger;
    }

    @Override
    public Query createQuery(String queryText) {
        Query query = new QueryWrapper(entityManager.createQuery(queryText));
        logger.log(String.format("Query Hash: %d\tHQL Query: %s", query.hashCode(), queryText));
        return query;
    }

    @Override
    public <T> T find(String queryText, Class<T> entityClass, Object primaryKey) {
        logger.log(String.format("Query Hash: %d\tHQL Query: %s", queryText.hashCode(), queryText));
        return entityManager.find(entityClass, primaryKey);
    }
}
