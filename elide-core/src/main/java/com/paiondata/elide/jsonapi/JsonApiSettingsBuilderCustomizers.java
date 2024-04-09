/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.core.dictionary.EntityDictionary;

/**
 * Utility methods for customizing a {@link JsonApiSettings.JsonApiSettingsBuilder}.
 */
public class JsonApiSettingsBuilderCustomizers {
    private JsonApiSettingsBuilderCustomizers() {
    }

    public static JsonApiSettings.JsonApiSettingsBuilder buildJsonApiSettingsBuilder(EntityDictionary entityDictionary,
                                                                                     JsonApiSettingsBuilderCustomizer customizer) {
        JsonApiSettings.JsonApiSettingsBuilder builder = JsonApiSettings.JsonApiSettingsBuilder.withDefaults(entityDictionary);
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder;
    }
}
