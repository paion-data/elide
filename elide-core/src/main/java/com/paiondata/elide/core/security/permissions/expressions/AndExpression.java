/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.permissions.expressions;

import com.paiondata.elide.core.security.permissions.ExpressionResult;

import lombok.Getter;

/**
 * Representation for an "And" expression.
 */
public class AndExpression implements Expression {
    @Getter
    private final Expression left;
    @Getter
    private final Expression right;

    /**
     * Constructor.
     *
     * @param left Left expression
     * @param right Right expression
     */
    public AndExpression(final Expression left, final Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public ExpressionResult evaluate(EvaluationMode mode) {
        ExpressionResult leftStatus = left.evaluate(mode);

        // Short-circuit
        if (leftStatus == ExpressionResult.FAIL) {
            return leftStatus;
        }

        ExpressionResult rightStatus = (right == null) ? ExpressionResult.PASS : right.evaluate(mode);

        if (rightStatus == ExpressionResult.FAIL) {
            return rightStatus;
        }

        if (leftStatus == ExpressionResult.PASS && rightStatus == ExpressionResult.PASS) {
            return ExpressionResult.PASS;
        }

        return ExpressionResult.DEFERRED;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitAndExpression(this);
    }

    @Override
    public String toString() {
        if (right == null) {
            return String.format("%s", left);

        }
        return String.format("(%s) AND (%s)", left, right);
    }
}
