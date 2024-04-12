/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.query;

import static com.paiondata.elide.core.utils.TypeHelper.getClassType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.datastores.aggregation.framework.SQLUnitTest;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.query.Optimizer;
import com.paiondata.elide.datastores.aggregation.query.Query;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SkipOptimizerTest extends SQLUnitTest {

    private static Optimizer optimizer = mock(Optimizer.class);

    @BeforeAll
    public static void init() {
        MetaDataStore metaDataStore = new MetaDataStore(
                new DefaultClassScanner(),
                getClassType(new DefaultClassScanner().getAnnotatedClasses("example",
                        Include.class)),
                false);

        Set<Optimizer> optimizers = new HashSet<>(Arrays.asList(optimizer));
        init(new H2Dialect(), optimizers, metaDataStore);
    }

    @BeforeEach
    public void beforeEach() {
        reset(optimizer);
    }

    @Test
    public void testSkippingOptimizer() {
        when(optimizer.hint()).thenReturn("Skip");
        Query query = TestQuery.WHERE_AND.getQuery();
        engine.explain(query);

        verify(optimizer, never()).optimize(any());
    }

    @Test
    public void testNegatingOptimizer() {
        when(optimizer.hint()).thenReturn("JoinBeforeAggregate");
        when(optimizer.negateHint()).thenReturn("NoJoinBeforeAggregate");
        Query query = TestQuery.WHERE_AND.getQuery();
        engine.explain(query);

        verify(optimizer, never()).optimize(any());
    }
}
