/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jms.websocket;

/**
 * Used to customize the mutable {@link SubscriptionWebSocketConfigurator.SubscriptionWebSocketConfiguratorBuilder}.
 */
public interface SubscriptionWebSocketConfiguratorBuilderCustomizer {
    public void customize(SubscriptionWebSocketConfigurator.SubscriptionWebSocketConfiguratorBuilder builder);
}
