/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.metadata;


import com.paiondata.elide.datastores.aggregation.query.ColumnProjection;
import com.paiondata.elide.datastores.aggregation.query.Queryable;
import com.paiondata.elide.core.request.Argument;
import com.github.jknack.handlebars.HandlebarsException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * Context for resolving args, expr etc under $$column. eg: {{$$column.args.arg1}}, {{$$column.expr}}.
 */
@Getter
@ToString
public class ColumnSubContext extends ColumnContext {

    @Builder(builderMethodName = "columnSubContextBuilder")
    public ColumnSubContext(MetaDataStore metaDataStore, Queryable queryable, String alias,
                            ColumnProjection column, Map<String, Argument> tableArguments) {
        super(metaDataStore, queryable, alias, column, tableArguments);
    }

    @Override
    public Object get(Object key) {

        if (key.equals(ARGS_KEY)) {
            return this.getColumn().getArguments();
        }

        if (key.equals(EXPR_KEY)) {
            return builder()
                            .queryable(this.getQueryable())
                            .alias(this.getAlias())
                            .metaDataStore(this.getMetaDataStore())
                            .column(this.getColumn())
                            .tableArguments(this.getTableArguments())
                            .build()
                            .resolve(this.getColumn().getExpression());
        }

        throw new HandlebarsException(new Throwable("Couldn't find: " + key));
    }
}
