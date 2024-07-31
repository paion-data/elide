/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service;

import com.paiondata.elide.async.service.AsyncProviderService.AsyncProviderServiceBuilder;

/**
 * Customizer for {@link AsyncProviderServiceBuilder}.
 */
public interface AsyncProviderServiceBuilderCustomizer {
    void customize(AsyncProviderServiceBuilder builder);
}
