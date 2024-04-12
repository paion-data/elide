/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.predicates;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.Path.PathElement;
import com.paiondata.elide.core.filter.Operator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NOT Insensitive Predicate class.
 */
public class NotInInsensitivePredicate extends FilterPredicate {

    public NotInInsensitivePredicate(Path path, List<Object> values) {
        super(path, Operator.NOT_INSENSITIVE, values);
    }

    @SafeVarargs
    public <T> NotInInsensitivePredicate(Path path, T... a) {
        this(path, Arrays.asList(a));
    }

    public NotInInsensitivePredicate(PathElement pathElement, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), values);
    }

    @SafeVarargs
    public <T> NotInInsensitivePredicate(PathElement pathElement, T... a) {
        this(pathElement, Arrays.asList(a));
    }
}
