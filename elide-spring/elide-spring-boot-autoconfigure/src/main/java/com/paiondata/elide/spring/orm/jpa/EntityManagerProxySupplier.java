/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa;

import com.paiondata.elide.datastores.jpa.JpaDataStore;

import jakarta.persistence.EntityManager;

/**
 * EntityManagerSupplier for Spring.
 */
public class EntityManagerProxySupplier implements JpaDataStore.EntityManagerSupplier {

    @Override
    public EntityManager get() {
        return new BasicEntityManagerProxy();
    }
}
