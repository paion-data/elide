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
import jakarta.persistence.OneToMany;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Tests the invocation &amp; sequencing of DataStoreTransaction method invocations and life cycle events.
 * Model used to mock different lifecycle test scenarios.  This model uses fields instead of properties.
 */
@Include(name = "testModel")
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreFlushHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreFlushHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHookEverything.class, operation = LifeCycleHookBinding.Operation.CREATE,
        phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT, oncePerRequest = false)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreFlushHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)

@EqualsAndHashCode
public class FieldTestModel {

    @Id
    private String id;

    @Getter
    @Setter
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreFlushHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreFlushHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreFlushHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
    private String field;

    @Getter
    @Setter
    @OneToMany
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreFlushHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreFlushHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = LifeCycleHookBinding.Operation.DELETE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreFlushHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.POSTCOMMIT)
    private Set<FieldTestModel> models = new HashSet<>();

    public static class ClassPreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, LifeCycleHookBinding.TransactionPhase.PRESECURITY);
        }
    }

    public static class ClassPreFlushHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, LifeCycleHookBinding.TransactionPhase.PREFLUSH);
        }
    }

    public static class ClassPreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, LifeCycleHookBinding.TransactionPhase.PRECOMMIT);
        }
    }

    public static class ClassPreCommitHookEverything implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classAllFieldsCallback(operation, LifeCycleHookBinding.TransactionPhase.PRECOMMIT);
        }
    }

    public static class ClassPostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, LifeCycleHookBinding.TransactionPhase.POSTCOMMIT);
        }
    }

    public static class AttributePreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, LifeCycleHookBinding.TransactionPhase.PRESECURITY, changes.orElse(null));
        }
    }

    public static class AttributePreFlushHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, LifeCycleHookBinding.TransactionPhase.PREFLUSH, changes.orElse(null));
        }
    }

    public static class AttributePreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, LifeCycleHookBinding.TransactionPhase.PRECOMMIT, changes.orElse(null));
        }
    }

    public static class AttributePostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, LifeCycleHookBinding.TransactionPhase.POSTCOMMIT, changes.orElse(null));
        }
    }

    public static class RelationPreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, LifeCycleHookBinding.TransactionPhase.PRESECURITY, changes.orElse(null));
        }
    }

    public static class RelationPreFlushHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, LifeCycleHookBinding.TransactionPhase.PREFLUSH, changes.orElse(null));
        }
    }

    public static class RelationPreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, LifeCycleHookBinding.TransactionPhase.PRECOMMIT, changes.orElse(null));
        }
    }

    public static class RelationPostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, LifeCycleHookBinding.TransactionPhase.POSTCOMMIT, changes.orElse(null));
        }
    }

    public void classCallback(LifeCycleHookBinding.Operation operation,
                              LifeCycleHookBinding.TransactionPhase phase) {
        //NOOP - this will be mocked to verify hook invocation.
    }

    public void attributeCallback(LifeCycleHookBinding.Operation operation,
                                  LifeCycleHookBinding.TransactionPhase phase,
                                  ChangeSpec changes) {
        //NOOP - this will be mocked to verify hook invocation.
    }

    public void relationCallback(LifeCycleHookBinding.Operation operation,
                                 LifeCycleHookBinding.TransactionPhase phase,
                                 ChangeSpec changes) {
        //NOOP - this will be mocked to verify hook invocation.
    }

    public void classAllFieldsCallback(LifeCycleHookBinding.Operation operation,
                                       LifeCycleHookBinding.TransactionPhase phase) {
        //NOOP - this will be mocked to verify hook invocation.
    }
}
