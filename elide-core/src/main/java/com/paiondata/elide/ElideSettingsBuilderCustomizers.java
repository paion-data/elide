/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

/**
 * Utility methods for customizing a {@link ElideSettings.ElideSettingsBuilder}.
 */
public class ElideSettingsBuilderCustomizers {
    private ElideSettingsBuilderCustomizers() {
    }

    public static ElideSettings.ElideSettingsBuilder buildElideSettingsBuilder(ElideSettingsBuilderCustomizer customizer) {
        ElideSettings.ElideSettingsBuilder builder = new ElideSettings.ElideSettingsBuilder();
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder;
    }
}
