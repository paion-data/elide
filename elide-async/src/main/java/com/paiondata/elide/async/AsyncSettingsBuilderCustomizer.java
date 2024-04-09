/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async;

/**
 * Used to customize the mutable {@link AsyncSettings.AsyncSettingsBuilder}.
 */
public interface AsyncSettingsBuilderCustomizer {
    public void customize(AsyncSettings.AsyncSettingsBuilder builder);
}
