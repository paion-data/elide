/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa.config;

import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.jpql.porting.QueryLogger;

import com.paiondata.elide.datastores.jpa.JpaDataStore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * Registration entry to configure a JpaDataStore.
 *
 * @see JpaDataStore
 */
@Builder
@AllArgsConstructor
public class JpaDataStoreRegistration {
    @Getter
    private final String name;
    @Getter
    private final JpaDataStore.EntityManagerSupplier entityManagerSupplier;
    @Getter
    private final JpaDataStore.JpaTransactionSupplier readTransactionSupplier;
    @Getter
    private final JpaDataStore.JpaTransactionSupplier writeTransactionSupplier;
    @Getter
    private final JpaDataStore.MetamodelSupplier metamodelSupplier;
    @Getter
    private final Set<Type<?>> managedClasses;
    @Getter
    private final QueryLogger queryLogger;

    /**
     * Used to build a JpaDataStore registration.
     */
    public static class JpaDataStoreRegistrationBuilder {
    }
}
