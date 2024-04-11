/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.permissions.expressions;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.OperationCheck;
import com.paiondata.elide.core.security.checks.UserCheck;
import com.paiondata.elide.core.security.permissions.ExpressionResult;
import com.paiondata.elide.core.security.permissions.ExpressionResultCache;
import com.paiondata.elide.core.dictionary.EntityDictionary;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * An expression in the security evaluation AST that wraps an actual check.
 */
@Slf4j
public class CheckExpression implements Expression {

    @Getter
    protected final Check check;
    protected final PersistentResource resource;
    protected final RequestScope requestScope;
    protected final ExpressionResultCache cache;
    protected ExpressionResult result;

    private final Optional<ChangeSpec> changeSpec;

    /**
     * Constructor.
     *
     * @param check The check to be evaluated by this expression
     * @param resource The resource to pass to the check
     * @param requestScope The requestScope to pass to the check
     * @param changeSpec The changeSpec to pass to the check
     * @param cache The cache of previous expression results
     */
    public CheckExpression(final Check check,
                           final PersistentResource resource,
                           final RequestScope requestScope,
                           final ChangeSpec changeSpec,
                           final ExpressionResultCache cache) {
        this.check = check;
        this.requestScope = requestScope;
        this.changeSpec = Optional.ofNullable(changeSpec);
        this.cache = cache;
        this.result = ExpressionResult.UNEVALUATED;

        // UserCheck does not use resource
        this.resource = (check instanceof UserCheck) ? null : resource;
    }

    @Override
    public ExpressionResult evaluate(EvaluationMode mode) {
        log.trace("Evaluating check: {} in mode {}", check, mode);

        /* Result evaluation is sticky once evaluated to PASS or FAIL */
        if (result == ExpressionResult.PASS || result == ExpressionResult.FAIL) {
            return result;
        }

        if (mode == EvaluationMode.USER_CHECKS_ONLY && ! (check instanceof UserCheck)) {
            result = ExpressionResult.DEFERRED;
            return result;
        }

        if (mode == EvaluationMode.INLINE_CHECKS_ONLY && (resource != null && resource.isNewlyCreated())) {
            result = ExpressionResult.DEFERRED;
            return result;
        }

        if (mode == EvaluationMode.INLINE_CHECKS_ONLY && check.runAtCommit()) {
            result = ExpressionResult.DEFERRED;
            return result;
        }

        // If we have a valid change spec, do not cache the result or look for a cached result.
        if (changeSpec.isPresent()) {
            log.trace("-- Check has changespec: {}", changeSpec);
            result = computeCheck();
            log.trace("-- Check returned with result: {}", result);
            return result;
        }

        // Otherwise, search the cache and use value if found. Otherwise, evaluate and add it to the cache.
        log.trace("-- Check does NOT have changespec");
        Class<? extends Check> checkClass = check.getClass();

        if (cache.hasStoredResultFor(checkClass, resource)) {
            result = cache.getResultFor(checkClass, resource);
        } else {
            result = computeCheck();
            cache.putResultFor(checkClass, resource, result);
            log.trace("-- Check computed result: {}", result);
        }

        log.trace("-- Check returned with result: {}", result);
        return result;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitCheckExpression(this);
    }

    /**
     * Actually compute the result of the check without caching concerns.
     *
     * @return Expression result from the check.
     */
    private ExpressionResult computeCheck() {
        Object entity = (resource == null) ? null : resource.getObject();

        if (check instanceof UserCheck) {
            result = ((UserCheck) check).ok(requestScope.getUser()) ? ExpressionResult.PASS : ExpressionResult.FAIL;
        } else {
            result = ((OperationCheck) check).ok(entity, requestScope, changeSpec) ? ExpressionResult.PASS : ExpressionResult.FAIL;
        }
        return result;
    }

    @Override
    public String toString() {
        EntityDictionary dictionary = ((com.paiondata.elide.core.RequestScope) requestScope).getDictionary();
        return String.format("(%s %s)", dictionary.getCheckIdentifier(check.getClass()), result);
    }
}
