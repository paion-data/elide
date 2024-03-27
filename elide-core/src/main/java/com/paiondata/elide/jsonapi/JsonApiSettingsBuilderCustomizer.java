/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

/**
 * Used to customize the mutable {@link JsonApiSettings.JsonApiSettingsBuilder}.
 */
public interface JsonApiSettingsBuilderCustomizer {
    public void customize(JsonApiSettings.JsonApiSettingsBuilder builder);
}
