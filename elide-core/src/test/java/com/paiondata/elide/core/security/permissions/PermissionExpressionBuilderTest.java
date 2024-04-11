/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.permissions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.prefab.Role;
import com.paiondata.elide.core.security.permissions.expressions.Expression;
import com.paiondata.elide.core.dictionary.TestDictionary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PermissionExpressionBuilderTest {

    private EntityDictionary dictionary;
    private PermissionExpressionBuilder builder;
    private ElideSettings elideSettings;

    @BeforeEach
    public void setupEntityDictionary() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("user has all access", Role.ALL.class);
        checks.put("user has no access", Role.NONE.class);

        dictionary = TestDictionary.getTestDictionary(checks);

        ExpressionResultCache cache = new ExpressionResultCache();
        builder = new PermissionExpressionBuilder(cache, dictionary);

        elideSettings = ElideSettings.builder().entityDictionary(dictionary)
                .build();
    }

    @Test
    public void testAnyFieldExpressionText() {
        @Entity
        @Include(rootLevel = false)
        @ReadPermission(expression = "user has all access AND user has no access")
        class Model { }
        dictionary.bindEntity(Model.class);

        PersistentResource resource = newResource(new Model(), Model.class);

        Expression expression = builder.buildAnyFieldExpressions(
                resource,
                ReadPermission.class,
                null, null);

        assertEquals("READ PERMISSION WAS INVOKED ON PersistentResource{type=model, id=null}  "
                        + "FOR EXPRESSION [((user has all access \u001B[34mWAS UNEVALUATED\u001B[m)) "
                        + "AND ((user has no access \u001B[34mWAS UNEVALUATED\u001B[m))]",
                expression.toString());

        expression.evaluate(Expression.EvaluationMode.ALL_CHECKS);

        assertEquals(
                "READ PERMISSION WAS INVOKED ON PersistentResource{type=model, id=null}  "
                        + "FOR EXPRESSION [((user has all access [32mPASSED[m)) "
                        + "AND ((user has no access [31mFAILED[m))]",
                expression.toString());

    }

    @Test
    public void testSpecificFieldExpressionText() {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "user has no access")
        class Model {
            @Id
            private long id;
            @UpdatePermission(expression = "user has all access OR user has no access")
            private int foo;
        }

        dictionary.bindEntity(Model.class);

        PersistentResource resource = newResource(new Model(), Model.class);
        ChangeSpec changes = new ChangeSpec(resource, "foo", 1, 2);

        Expression expression = builder.buildSpecificFieldExpressions(
                resource,
                UpdatePermission.class,
                "foo",
                changes);

        assertEquals(
                "UPDATE PERMISSION WAS INVOKED ON PersistentResource{type=model, id=0} WITH CHANGES ChangeSpec { "
                        + "resource=PersistentResource{type=model, id=0}, field=foo, original=1, modified=2} "
                        + "FOR EXPRESSION [FIELD(((user has all access "
                        + "\u001B[34mWAS UNEVALUATED\u001B[m)) OR ((user has no access "
                        + "\u001B[34mWAS UNEVALUATED\u001B[m)))]",
                expression.toString());

        expression.evaluate(Expression.EvaluationMode.ALL_CHECKS);

        assertEquals(
                "UPDATE PERMISSION WAS INVOKED ON PersistentResource{type=model, id=0} WITH CHANGES ChangeSpec { "
                        + "resource=PersistentResource{type=model, id=0}, field=foo, original=1, modified=2} "
                        + "FOR EXPRESSION [FIELD(((user has all access "
                        + "\u001B[32mPASSED\u001B[m)) OR ((user has no access "
                        + "\u001B[34mWAS UNEVALUATED\u001B[m)))]",
                expression.toString());

     }

    public <T> PersistentResource newResource(T obj, Class<T> cls) {
        Route route = Route.builder().apiVersion(EntityDictionary.NO_VERSION).build();
        RequestScope requestScope = RequestScope.builder().route(route).requestId(UUID.randomUUID())
                .elideSettings(elideSettings).build();
        return new PersistentResource<>(obj, requestScope.getUUIDFor(obj), requestScope);
    }
}
