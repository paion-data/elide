/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.security.TestUser;
import example.Child;
import example.FunWithPermissions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PersistentResourceNoopUpdateTest extends PersistenceResourceTestSetup {
    private final RequestScope goodUserScope;
    private final User goodUser;
    PersistentResourceNoopUpdateTest() {
        goodUser = new TestUser("1");
        goodUserScope = RequestScope.builder().route(Route.builder().apiVersion(EntityDictionary.NO_VERSION).build())
                .dataStoreTransaction(Mockito.mock(DataStoreTransaction.class)).user(goodUser).requestId(UUID.randomUUID())
                .elideSettings(elideSettings).build();
        initDictionary();
        reset(goodUserScope.getTransaction());
    }

    @Test
    public void testNOOPToOneAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        fun.setRelation3(child);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = RequestScope.builder().route(Route.builder().apiVersion(EntityDictionary.NO_VERSION).build())
                .dataStoreTransaction(tx).user(goodUser).requestId(UUID.randomUUID()).elideSettings(elideSettings)
                .build();
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);

        when(tx.getToOneRelation(eq(tx), eq(fun), any(), any())).thenReturn(child);

        //We do not want the update to one method to be called when we add the existing entity to the relation
        funResource.addRelation("relation3", childResource);

        verify(tx, never()).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
    }

    @Test
    public void testToOneAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = RequestScope.builder().route(Route.builder().apiVersion(EntityDictionary.NO_VERSION).build())
                .dataStoreTransaction(tx).user(goodUser).requestId(UUID.randomUUID()).elideSettings(elideSettings)
                .build();
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, "1", goodScope);
        funResource.addRelation("relation3", childResource);

        verify(tx, times(1)).updateToOneRelation(eq(tx), eq(fun), any(), any(), eq(goodScope));
    }

    @Test
    public void testNOOPToManyAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);
        Set<Child> children = new HashSet<>();
        children.add(child);
        fun.setRelation1(children);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = RequestScope.builder().route(Route.builder().apiVersion(EntityDictionary.NO_VERSION).build())
                .dataStoreTransaction(tx).user(goodUser).requestId(UUID.randomUUID()).elideSettings(elideSettings)
                .build();
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, goodScope);
        //We do not want the update to one method to be called when we add the existing entity to the relation
        funResource.addRelation("relation1", childResource);
        verify(tx, never()).updateToManyRelation(eq(tx), eq(child), eq("relation1"), any(), any(), eq(goodScope));
    }

    @Test
    public void testToManyAddRelation() {
        FunWithPermissions fun = new FunWithPermissions();
        Child child = newChild(1);

        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = RequestScope.builder().route(Route.builder().apiVersion(EntityDictionary.NO_VERSION).build())
                .dataStoreTransaction(tx).user(goodUser).requestId(UUID.randomUUID()).elideSettings(elideSettings)
                .build();
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", goodScope);
        PersistentResource<Child> childResource = new PersistentResource<>(child, null, goodScope);
        funResource.addRelation("relation1", childResource);
        verify(tx, times(1)).updateToManyRelation(eq(tx), eq(fun), eq("relation1"), any(), any(), eq(goodScope));
    }
}
