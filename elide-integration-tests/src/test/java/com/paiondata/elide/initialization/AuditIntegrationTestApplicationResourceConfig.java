/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.initialization;

import com.paiondata.elide.core.audit.InMemoryLogger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.inject.Inject;

/**
 * Resource config for Audit IT tests.
 */
public class AuditIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public static final InMemoryLogger LOGGER = new InMemoryLogger();

    @Inject
    public AuditIntegrationTestApplicationResourceConfig(ServiceLocator injector) {
        register(new StandardTestBinder(LOGGER, injector));
    }
}
