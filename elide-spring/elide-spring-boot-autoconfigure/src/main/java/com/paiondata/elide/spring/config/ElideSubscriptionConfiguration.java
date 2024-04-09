/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.paiondata.elide.ElideMapper;
import com.paiondata.elide.ElideSettingsBuilderCustomizer;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.datastores.jms.websocket.SubscriptionWebSocketConfigurator;
import com.paiondata.elide.datastores.jms.websocket.SubscriptionWebSocketConfiguratorBuilderCustomizer;
import com.paiondata.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;

import com.paiondata.elide.Serdes;
import com.paiondata.elide.Settings;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import graphql.execution.DataFetcherExceptionHandler;

import jakarta.jms.ConnectionFactory;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Configures GraphQL subscription web sockets for Elide.
 */
@Configuration
@ConditionalOnProperty(name = "elide.graphql.enabled", havingValue = "true")
@EnableConfigurationProperties(ElideConfigProperties.class)
public class ElideSubscriptionConfiguration {
    /**
     * Exposes a subscription {@link ServerEndpointConfig} that doesn't accept a
     * path parameter for api versioning.
     *
     * @param config  the config
     * @param builder the builder
     * @return the config
     */
    @Bean
    @ConditionalOnProperty(name = "elide.graphql.subscription.enabled", havingValue = "true")
    ServerEndpointConfig serverEndpointConfig(ElideConfigProperties config,
            SubscriptionWebSocketConfigurator.SubscriptionWebSocketConfiguratorBuilder builder) {
        String path = config.getGraphql().getSubscription().getPath();
        return ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, path)
                .subprotocols(SubscriptionWebSocket.SUPPORTED_WEBSOCKET_SUBPROTOCOLS)
                .configurator(builder.build())
                .build();
    }

    /**
     * Exposes a subscription {@link ServerEndpointConfig} that accepts a path
     * parameter for api versioning.
     *
     * @param config  the config
     * @param builder the builder
     * @return the config
     */
    @Bean
    @ConditionalOnProperty(name = "elide.graphql.subscription.enabled", havingValue = "true")
    ServerEndpointConfig serverEndpointConfigPath(ElideConfigProperties config,
            SubscriptionWebSocketConfigurator.SubscriptionWebSocketConfiguratorBuilder builder) {
        String path = config.getGraphql().getSubscription().getPath();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        path = path + "{path}";
        return ServerEndpointConfig.Builder
                .create(SubscriptionWebSocket.class, path)
                .subprotocols(SubscriptionWebSocket.SUPPORTED_WEBSOCKET_SUBPROTOCOLS)
                .configurator(builder.build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.graphql.subscription.enabled", havingValue = "true")
    @Scope(SCOPE_PROTOTYPE)
    SubscriptionWebSocketConfigurator.SubscriptionWebSocketConfiguratorBuilder subscriptionWebSocketConfiguratorBuilder(
            ElideConfigProperties config,
            SubscriptionWebSocket.UserFactory userFactory,
            ConnectionFactory connectionFactory,
            DataFetcherExceptionHandler dataFetcherExceptionHandler,
            RouteResolver routeResolver,
            ObjectProvider<SubscriptionWebSocketConfiguratorBuilderCustomizer> customizers,
            Injector injector,
            Serdes.SerdesBuilder serdesBuilder,
            ElideMapper elideMapper,
            AuditLogger auditLogger,
            ObjectProvider<Settings.SettingsBuilder> settingsProvider,
            ObjectProvider<ElideSettingsBuilderCustomizer> customizerProvider
            ) {
        SubscriptionWebSocketConfigurator.SubscriptionWebSocketConfiguratorBuilder builder = SubscriptionWebSocketConfigurator.builder()
                .baseUrl(config.getGraphql().getSubscription().getPath())
                .sendPingOnSubscribe(config.getGraphql().getSubscription().isSendPingOnSubscribe())
                .connectionTimeout(config.getGraphql().getSubscription().getConnectionTimeout())
                .maxSubscriptions(config.getGraphql().getSubscription().maxSubscriptions)
                .maxMessageSize(config.getGraphql().getSubscription().maxMessageSize)
                .maxIdleTimeout(config.getGraphql().getSubscription().getIdleTimeout())
                .connectionFactory(connectionFactory)
                .userFactory(userFactory)
                .elideSettingsBuilderCustomizer(elideSettingsBuilder -> {
                    elideSettingsBuilder.serdes(serdes -> serdes.entries(entries -> {
                        entries.clear();
                        serdesBuilder.build().entrySet().stream().forEach(entry -> {
                            entries.put(entry.getKey(), entry.getValue());
                        });
                    })).objectMapper(elideMapper.getObjectMapper()).auditLogger(auditLogger)
                            .verboseErrors(config.isVerboseErrors())
                            .maxPageSize(config.getMaxPageSize())
                            .defaultPageSize(config.getDefaultPageSize());
                    settingsProvider.orderedStream().forEach(elideSettingsBuilder::settings);
                    customizerProvider.orderedStream()
                            .forEach(customizer -> customizer.customize(elideSettingsBuilder));
                })
                .dataFetcherExceptionHandler(dataFetcherExceptionHandler)
                .routeResolver(routeResolver)
                .injector(injector);
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.graphql.subscription.enabled", havingValue = "true")
    ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        return exporter;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "elide.graphql.subscription.enabled", havingValue = "true")
    SubscriptionWebSocket.UserFactory userFactory() {
        return SubscriptionWebSocket.DEFAULT_USER_FACTORY;
    }
}
