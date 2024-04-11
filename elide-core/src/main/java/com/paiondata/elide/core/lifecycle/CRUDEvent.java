/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.lifecycle;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;

import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.PersistentResource;
import com.paiondata.elide.annotation.LifeCycleHookBinding;

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
        return eventType == CREATE;
    }

    public boolean isUpdateEvent() {
        return eventType == UPDATE;
    }

    public boolean isDeleteEvent() {
        return eventType == DELETE;
    }
}
