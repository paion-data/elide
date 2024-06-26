/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.predicates;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.Path.PathElement;
import com.paiondata.elide.core.filter.Operator;

import java.util.Collections;

/**
 * LT Predicate class.
 */
public class LTPredicate extends FilterPredicate {

    public LTPredicate(Path path, Object value) {
        super(path, Operator.LT, Collections.singletonList(value));
    }

    public LTPredicate(PathElement pathElement, Object value) {
        this(new Path(Collections.singletonList(pathElement)), value);
    }
}
