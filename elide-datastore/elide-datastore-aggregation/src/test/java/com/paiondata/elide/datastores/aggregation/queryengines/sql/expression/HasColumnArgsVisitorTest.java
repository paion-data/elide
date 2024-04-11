/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.expression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.paiondata.elide.datastores.aggregation.annotation.ArgumentDefinition;
import com.paiondata.elide.datastores.aggregation.annotation.DimensionFormula;
import com.paiondata.elide.datastores.aggregation.annotation.Join;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.metadata.enums.ValueType;
import com.paiondata.elide.datastores.aggregation.query.ColumnProjection;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Id;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

public class HasColumnArgsVisitorTest {

    private MetaDataStore metaDataStore;

    @Include
    class TableA {
        @Id
        long id;

        @Join(value = "{{$$column.args.foo}} = '123' AND {{$id}} == {{joinWithColumnArgs.$id}}")
        TableB joinWithColumnArgs;

        @DimensionFormula(value = "{{joinWithColumnArgs.physical}}",
                          arguments = {@ArgumentDefinition(name = "foo", type = ValueType.TEXT)})
        String joinWithColumnArgsToPhysical;

        @DimensionFormula(value = "{{physical}}")
        String logical;

        @DimensionFormula(value = "{{$physical}}")
        String physical;
    }

    @Include
    class TableB {
        @Id
        long id;

        @DimensionFormula(value = "{{$physical}}")
        String physical;
    }

    @Include
    class TableC {
        @Id
        long id;

        @Join(value = "{{$id}} == {{simpleJoin.$id}}")
        TableA simpleJoin;

        @Join(value = "{{$id}} == {{simpleJoin.$id}} AND {{physical}} = '123'")
        TableB joinWithPhysical;

        @Join(value = "{{$id}} == {{simpleJoin.$id}} AND {{columnArgs}} = '123'")
        TableB joinWithLogicalWithColumnArgs;

        @DimensionFormula(value = "{{simpleJoin.joinWithColumnArgs.physical}}",
                          arguments = {@ArgumentDefinition(name = "foo", type = ValueType.TEXT)})
        String nestedJoinWithColumnArgs;

        @DimensionFormula(value = "{{simpleJoin.logical}}")
        String simpleJoinToLogical;

        @DimensionFormula(value = "{{$physical}}")
        String physical;

        @DimensionFormula(value = "{{$$column.args.foo}}",
                          arguments = {@ArgumentDefinition(name = "foo", type = ValueType.TEXT)})
        String columnArgs;

        @DimensionFormula(value = "{{joinWithLogicalWithColumnArgs.physical}}",
                          arguments = {@ArgumentDefinition(name = "foo", type = ValueType.TEXT)})
        String joinWithLogicalWithColumnArgsToPhysical;

        @DimensionFormula(value = "{{joinWithPhysical.physical}}")
        String joinWithPhysicalToPhysical;
    }

    public HasColumnArgsVisitorTest() {

        Set<Type<?>> models = new HashSet<>();
        models.add(ClassType.of(TableA.class));
        models.add(ClassType.of(TableB.class));
        models.add(ClassType.of(TableC.class));

        EntityDictionary dictionary = EntityDictionary.builder().build();

        metaDataStore = new MetaDataStore(dictionary.getScanner(), models, true);
        metaDataStore.populateEntityDictionary(dictionary);

        DataSource mockDataSource = mock(DataSource.class);
        //The query engine populates the metadata store with actual tables.
        new SQLQueryEngine(metaDataStore, (unused) -> new ConnectionDetails(mockDataSource,
                SQLDialectFactory.getDefaultDialect()));
    }


    @Test
    public void testJoinWithColumnArgs() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableA.class));
        ColumnProjection projection = table.getColumnProjection("joinWithColumnArgsToPhysical");
        assertTrue(matches(table, projection));
    }

    @Test
    public void testPhysicalColumn() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableB.class));
        ColumnProjection projection = table.getColumnProjection("physical");
        assertFalse(matches(table, projection));
    }

    @Test
    public void testNestedJoinWithColumnArgs() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableC.class));
        ColumnProjection projection = table.getColumnProjection("nestedJoinWithColumnArgs");
        assertTrue(matches(table, projection));
    }

    @Test
    public void testSimpleJoinToLogicalColumn() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableC.class));
        ColumnProjection projection = table.getColumnProjection("simpleJoinToLogical");
        assertFalse(matches(table, projection));
    }

    @Test
    public void testColumnArgs() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableC.class));
        ColumnProjection projection = table.getColumnProjection("columnArgs");
        assertTrue(matches(table, projection));
    }

    @Test
    public void testJoinWithLogicalReferenceThatReferencesColumnArgs() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableC.class));
        ColumnProjection projection = table.getColumnProjection("joinWithLogicalWithColumnArgsToPhysical");
        assertTrue(matches(table, projection));
    }

    @Test
    public void testJoinWithLogicalReferenceThatReferencesPhysical() throws Exception {
        SQLTable table = metaDataStore.getTable(ClassType.of(TableC.class));
        ColumnProjection projection = table.getColumnProjection("joinWithPhysicalToPhysical");
        assertFalse(matches(table, projection));
    }

    private boolean matches(SQLTable source, ColumnProjection projection) {
        List<Reference> references = new ExpressionParser(metaDataStore).parse(source, projection);

        return ! references.stream()
                .map(reference -> reference.accept(new ReferenceExtractor<ColumnArgReference>(
                        ColumnArgReference.class, metaDataStore)))
                .flatMap(Set::stream)
                .collect(Collectors.toSet())
                .isEmpty();
    }
}
