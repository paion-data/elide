/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.lifecycle;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.PersistentResource;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

/**
 * Captures all the bits related to a CRUD operation on a model.
 */
@Data
@AllArgsConstructor
public class CRUDEvent {
    private LifeCycleHookBinding.Operation eventType;
    private PersistentResource resource;
    private String fieldName;
    private Optional<ChangeSpec> changes;

    public boolean isCreateEvent() {
        return eventType == LifeCycleHookBinding.Operation.CREATE;
    }

    public boolean isUpdateEvent() {
        return eventType == LifeCycleHookBinding.Operation.UPDATE;
    }

    public boolean isDeleteEvent() {
        return eventType == LifeCycleHookBinding.Operation.DELETE;
    }
}
