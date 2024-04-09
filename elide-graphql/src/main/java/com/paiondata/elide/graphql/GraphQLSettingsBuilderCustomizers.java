/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.core.dictionary.EntityDictionary;

/**
 * Utility methods for customizing a {@link GraphQLSettings.GraphQLSettingsBuilder}.
 */
public class GraphQLSettingsBuilderCustomizers {
    private GraphQLSettingsBuilderCustomizers() {
    }

    public static GraphQLSettings.GraphQLSettingsBuilder buildGraphQLSettingsBuilder(EntityDictionary entityDictionary,
                                                                                     GraphQLSettingsBuilderCustomizer customizer) {
        GraphQLSettings.GraphQLSettingsBuilder builder = GraphQLSettings.GraphQLSettingsBuilder.withDefaults(entityDictionary);
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder;
    }
}
