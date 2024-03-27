/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.lifecycle;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Model used to mock different lifecycle test scenarios.  This model uses properties instead of fields.
 */
@Include
public class PropertyTestModel {
    private String id;

    private Set<PropertyTestModel> models = new HashSet<>();

    public static class RelationPostCommitHook implements LifeCycleHook<PropertyTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            PropertyTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, LifeCycleHookBinding.TransactionPhase.POSTCOMMIT, changes.orElse(null));
        }
    }

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ManyToMany
    @LifeCycleHookBinding(hook = PropertyTestModel.RelationPostCommitHook.class,
            operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
    @LifeCycleHookBinding(hook = PropertyTestModel.RelationPostCommitHook.class,
            operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
    public Set<PropertyTestModel> getModels() {
        return models;
    }

    public void setModels(Set<PropertyTestModel> models) {
        this.models = models;
    }

    public void relationCallback(LifeCycleHookBinding.Operation operation,
                                 LifeCycleHookBinding.TransactionPhase phase,
                                 ChangeSpec changes) {
        //NOOP - this will be mocked to verify hook invocation.
    }
}
