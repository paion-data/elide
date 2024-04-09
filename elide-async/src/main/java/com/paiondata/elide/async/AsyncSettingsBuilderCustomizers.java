/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async;

/**
 * Utility methods for customizing a {@link AsyncSettings.AsyncSettingsBuilder}.
 */
public class AsyncSettingsBuilderCustomizers {
    private AsyncSettingsBuilderCustomizers() {
    }

    public static AsyncSettings.AsyncSettingsBuilder buildAsyncSettingsBuilder(AsyncSettingsBuilderCustomizer customizer) {
        AsyncSettings.AsyncSettingsBuilder builder = new AsyncSettings.AsyncSettingsBuilder();
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder;
    }
}
