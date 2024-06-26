/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.example.other;

import com.paiondata.elide.annotation.Include;

import jakarta.persistence.Entity;

/**
 * Exclude Test bean.
 */
@Entity
@Include(rootLevel = false)
public class OtherBean {
    private boolean excluded;
}
