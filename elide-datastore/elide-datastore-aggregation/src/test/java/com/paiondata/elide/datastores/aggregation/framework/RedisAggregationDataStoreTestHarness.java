/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.framework;

import com.paiondata.elide.datastores.aggregation.AggregationDataStore;
import com.paiondata.elide.datastores.aggregation.cache.RedisCache;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.paiondata.elide.datastores.multiplex.MultiplexManager;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.modelconfig.validator.DynamicConfigValidator;

import jakarta.persistence.EntityManagerFactory;
import redis.clients.jedis.JedisPooled;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

public class RedisAggregationDataStoreTestHarness extends AggregationDataStoreTestHarness {
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    private static final int EXPIRATION_MINUTES = 2;

    public RedisAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, ConnectionDetails defaultConnectionDetails,
            Map<String, ConnectionDetails> connectionDetailsMap, DynamicConfigValidator validator) {
        super(entityManagerFactory, defaultConnectionDetails, connectionDetailsMap, validator);
    }

    public RedisAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, DataSource defaultDataSource) {
        super(entityManagerFactory, new ConnectionDetails(defaultDataSource, SQLDialectFactory.getDefaultDialect()));
    }

    public RedisAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory,
                    ConnectionDetails defaultConnectionDetails) {
        super(entityManagerFactory, defaultConnectionDetails, Collections.emptyMap(), null);
    }

    @Override
    public DataStore getDataStore() {
        JedisPooled jedisPool = new JedisPooled(HOST, PORT);
        RedisCache cache = new RedisCache(jedisPool, EXPIRATION_MINUTES);

        MetaDataStore metaDataStore = createMetaDataStore();

        AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = createAggregationDataStoreBuilder(metaDataStore)
                .cache(cache);

        DataStore jpaStore = createJPADataStore();

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStoreBuilder.build());
    }
}
