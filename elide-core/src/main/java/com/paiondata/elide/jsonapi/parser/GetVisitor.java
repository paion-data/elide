/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.parser;

import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.generated.parsers.CoreParser.QueryContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

/**
 * GET handler.
 */
public class GetVisitor extends BaseVisitor {

    /**
     * Constructor.
     *
     * @param requestScope the request scope
     */
    public GetVisitor(JsonApiRequestScope requestScope) {
        super(requestScope);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> visitQuery(QueryContext ctx) {
        return state.handleGet();
    }
}
