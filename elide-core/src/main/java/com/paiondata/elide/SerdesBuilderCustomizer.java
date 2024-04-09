/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

/**
 * Used to customize the mutable {@link Serdes.SerdesBuilder}.
 */
public interface SerdesBuilderCustomizer {
    void customize(Serdes.SerdesBuilder builder);
}
