/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pre-create hook. This annotation marks a callback that is triggered when a user performs a "create" action.
 * This hook will be triggered <em>after</em> all security checks have been run, but <em>before</em> the datastore
 * has been committed.
 *
 * The invoked function takes a RequestScope as parameter.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface OnCreatePreCommit {
    /**
     * Field name on which the annotated method is only triggered if that field is modified.
     * If value is empty string, then trigger once when the object is created.
     * If value is "*", then this method will be triggered once for each field that
     * the user sent in the creation request.
     *
     * @return the field name that triggers the method
     */
    String value() default "";
}
