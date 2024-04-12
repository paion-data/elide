/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.extensions;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;

import java.util.UUID;

/**
 * The request scope for the JSON API JSON Patch extension.
 */
public class JsonApiJsonPatchRequestScope extends JsonApiRequestScope {

    /**
     * Outer RequestScope constructor for use by Patch Extension.
     *
     * @param route         the route
     * @param transaction   current transaction
     * @param user          request user
     * @param requestId     request ID
     * @param elideSettings Elide settings object
     */
    public JsonApiJsonPatchRequestScope(
            Route route,
            DataStoreTransaction transaction,
            User user,
            UUID requestId,
            ElideSettings elideSettings) {
        super(
                route,
                transaction,
                user,
                requestId,
                elideSettings,
                null,
                null
        );
    }

    /**
     * Inner RequestScope copy constructor for use by Patch Extension actions.
     *
     * @param path            the URL path
     * @param jsonApiDocument document
     * @param scope           outer request scope
     */
    public JsonApiJsonPatchRequestScope(String path, JsonApiDocument jsonApiDocument,
            JsonApiJsonPatchRequestScope scope) {
        super(scope.getRoute().mutate().path(path).build(), jsonApiDocument, scope);
    }
}
