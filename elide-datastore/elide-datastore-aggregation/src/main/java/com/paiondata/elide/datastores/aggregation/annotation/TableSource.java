/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

import com.paiondata.elide.datastores.aggregation.dynamic.NamespacePackage;

/**
 * The definition of TableSource.
 */
public @interface TableSource {
    String table();
    String namespace() default NamespacePackage.DEFAULT;
    String column();
    String [] suggestionColumns() default {};
}
