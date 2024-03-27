/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.lifecycle;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;

import jakarta.persistence.Id;

import java.util.Optional;

/**
 * Tests life cycle hooks which raise errors.
 */
@Include(name = "errorTestModel")
@LifeCycleHookBinding(hook = ErrorTestModel.ErrorHook.class, operation = LifeCycleHookBinding.Operation.CREATE, phase = LifeCycleHookBinding.TransactionPhase.PRECOMMIT)
public class ErrorTestModel {

    @Id
    private String id;

    private String field;

    public static class ErrorHook implements LifeCycleHook<ErrorTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            ErrorTestModel elideEntity,
                            RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            throw new BadRequestException("Invalid");
        }
    }
}
