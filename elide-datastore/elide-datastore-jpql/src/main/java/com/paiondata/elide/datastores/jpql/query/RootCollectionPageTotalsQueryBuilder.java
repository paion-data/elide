/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpql.query;

import static com.paiondata.elide.core.utils.TypeHelper.getTypeAlias;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.PredicateExtractionVisitor;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.jpql.filter.FilterTranslator;
import com.paiondata.elide.datastores.jpql.porting.Query;
import com.paiondata.elide.datastores.jpql.porting.Session;

import java.util.Collection;
import java.util.HashSet;

/**
 * Constructs a HQL query to fetch the size of a root collection.
 */
public class RootCollectionPageTotalsQueryBuilder extends AbstractHQLQueryBuilder {

    public RootCollectionPageTotalsQueryBuilder(EntityProjection entityProjection,
                                                EntityDictionary dictionary,
                                                Session session) {
        super(entityProjection, dictionary, session);
    }

    /**
     * Constructs a query that returns the count of a root collection.
     *
     * Constructs a query like:
     *
     * SELECT COUNT(DISTINCT Author)
     * FROM Author AS Author
     *
     * @return the constructed query
     */
    @Override
    public Query build() {
        Type<?> entityClass = entityProjection.getType();
        String entityName = entityClass.getCanonicalName();
        String entityAlias = getTypeAlias(entityClass);

        Collection<FilterPredicate> predicates;

        String filterClause;
        String joinClause;

        FilterExpression filterExpression = entityProjection.getFilterExpression();
        if (filterExpression != null) {
            PredicateExtractionVisitor extractor = new PredicateExtractionVisitor();
            predicates = filterExpression.accept(extractor);

            //Build the WHERE clause
            filterClause = WHERE + new FilterTranslator(dictionary).apply(filterExpression, USE_ALIAS);

            //Build the JOIN clause
            joinClause =  getJoinClauseFromFilters(filterExpression, true);

        } else {
            predicates = new HashSet<>();
            filterClause = "";
            joinClause = "";
        }

        boolean requiresDistinct = joinClause != null && !joinClause.isEmpty();

        Query query = session.createQuery("SELECT COUNT(" + (requiresDistinct ? DISTINCT  + " " : "")
                + entityAlias
                + ") "
                + FROM
                + entityName
                + AS
                + entityAlias
                + SPACE
                + joinClause
                + SPACE
                + filterClause
        );
        supplyFilterQueryParameters(query, predicates);
        return query;
    }
}
