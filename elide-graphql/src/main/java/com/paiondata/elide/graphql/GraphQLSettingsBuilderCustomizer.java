/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

/**
 * Used to customize the mutable {@link GraphQLSettings.GraphQLSettingsBuilder}.
 */
public interface GraphQLSettingsBuilderCustomizer {
    public void customize(GraphQLSettings.GraphQLSettingsBuilder builder);
}
