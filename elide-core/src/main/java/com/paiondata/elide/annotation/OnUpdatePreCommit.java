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
 * Pre-update hook. This annotation marks a callback that is triggered when a user performs a "update" action.
 * This hook will be triggered <em>after</em> all security checks have been run, but <em>before</em> the datastore
 * has been committed.
 * <p>
 * The invoked function takes a RequestScope as parameter.
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface OnUpdatePreCommit {
    /**
     * Field name on which the annotated method is only triggered if that field is modified.
     * If value is empty string, then trigger once when the object is updated.
     * If value is "*", then trigger for all field modifications.
     *
     * @return the field name that triggers the method
     */
    String value() default "";
}
