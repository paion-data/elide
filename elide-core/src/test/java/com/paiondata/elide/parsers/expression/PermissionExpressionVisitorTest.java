/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.parsers.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.TestDictionary;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.OperationCheck;
import com.paiondata.elide.core.security.checks.UserCheck;
import com.paiondata.elide.core.security.checks.prefab.Role;
import com.paiondata.elide.core.security.permissions.ExpressionResult;
import com.paiondata.elide.core.security.permissions.expressions.Expression;
import com.paiondata.elide.core.security.permissions.expressions.ExpressionVisitor;
import com.paiondata.elide.core.security.visitors.PermissionExpressionVisitor;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test the expression language.
 */
public class PermissionExpressionVisitorTest {
    private EntityDictionary dictionary;

    @BeforeEach
    public void setupEntityDictionary() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("Allow", Permissions.Succeeds.class);
        checks.put("Deny", Permissions.Fails.class);
        checks.put("user has all access", Role.ALL.class);
        checks.put("user has no access", Role.NONE.class);

        dictionary = TestDictionary.getTestDictionary(checks);
        dictionary.bindEntity(Model.class);
        dictionary.bindEntity(ComplexEntity.class);
    }

    @Test
    public void testAndExpression() {
        Expression expression = getExpressionForPermission(ReadPermission.class);
        Assertions.assertEquals(ExpressionResult.PASS, expression.evaluate(Expression.EvaluationMode.ALL_CHECKS));
    }

    @Test
    public void testOrExpression() {
        Expression expression = getExpressionForPermission(UpdatePermission.class);
        Assertions.assertEquals(ExpressionResult.PASS, expression.evaluate(Expression.EvaluationMode.ALL_CHECKS));
    }

    @Test
    public void testNotExpression() {
        Expression expression = getExpressionForPermission(DeletePermission.class);
        Assertions.assertEquals(ExpressionResult.PASS, expression.evaluate(Expression.EvaluationMode.ALL_CHECKS));
    }

    @Test
    public void testComplexExpression() {
        Expression expression = getExpressionForPermission(UpdatePermission.class);
        Assertions.assertEquals(ExpressionResult.PASS, expression.evaluate(Expression.EvaluationMode.ALL_CHECKS));
    }

    @Test
    public void testComplexModelCreate() {
        Expression expression = getExpressionForPermission(CreatePermission.class, ClassType.of(ComplexEntity.class));
        Assertions.assertEquals(ExpressionResult.PASS, expression.evaluate(Expression.EvaluationMode.ALL_CHECKS));
    }

    @Test
    public void testNamesWithSpaces() {
        Expression expression = getExpressionForPermission(DeletePermission.class, ClassType.of(ComplexEntity.class));
        Expression expression2 = getExpressionForPermission(UpdatePermission.class, ClassType.of(ComplexEntity.class));
        Assertions.assertEquals(ExpressionResult.PASS, expression.evaluate(Expression.EvaluationMode.ALL_CHECKS));
        Assertions.assertEquals(ExpressionResult.PASS, expression2.evaluate(Expression.EvaluationMode.ALL_CHECKS));
    }

    private Expression getExpressionForPermission(Class<? extends Annotation> permission) {
        return getExpressionForPermission(permission, ClassType.of(Model.class));
    }

    private Expression getExpressionForPermission(Class<? extends Annotation> permission, Type model) {
        PermissionExpressionVisitor v = new PermissionExpressionVisitor(dictionary, DummyExpression::new);
        ParseTree permissions = dictionary.getPermissionsForClass(model, permission);

        return v.visit(permissions);
    }

    @Entity
    @Include(rootLevel = false)
    @ReadPermission(expression = "user has all access AND Allow")
    @UpdatePermission(expression = "Allow or Deny")
    @DeletePermission(expression = "Not Deny")
    @CreatePermission(expression = "not Allow or not (Deny and Allow)")
    static class Model {
    }

    public static class Permissions {
        public static class Succeeds extends OperationCheck<Model> {
            @Override
            public boolean ok(Model object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
                return true;
            }
        }

        public static class Fails extends OperationCheck<Model> {
            @Override
            public boolean ok(Model object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
                return false;
            }
        }
    }

    @Entity
    @Include(rootLevel = false)
    @CreatePermission(expression = "(Deny or Allow) and (not Deny)")
    @DeletePermission(expression = "user has all access or user has no access")
    @UpdatePermission(expression = "user has all access and (user has no access or user has all access)")
    static class ComplexEntity {
    }

    @AllArgsConstructor
    public static class DummyExpression implements Expression {
        Check check;

        @Override
        public ExpressionResult evaluate(EvaluationMode ignored) {
            boolean result;
            if (check instanceof UserCheck) {
                result = ((UserCheck) check).ok(null);
            } else {
                result = ((OperationCheck) check).ok(null, null, null);
            }

            if (result) {
                return ExpressionResult.PASS;
            }
            return ExpressionResult.FAIL;
        }

        @Override
        public <T> T accept(ExpressionVisitor<T> visitor) {
            return null;
        }
    }
}
