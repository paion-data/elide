/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jpql.query;

import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.type.Type;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds all the properties of an entity relationship.
 */
@Data
@AllArgsConstructor
public class RelationshipImpl implements AbstractHQLQueryBuilder.Relationship {
    private Type<?> parentType;
    private Object parent;
    private Relationship relationship;
}
