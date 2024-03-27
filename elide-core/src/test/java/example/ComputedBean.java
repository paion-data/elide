/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.annotation.ComputedAttribute;
import com.paiondata.elide.annotation.Include;

import jakarta.persistence.Entity;

/**
 * Bean with only computed fields.
 */
@Include(rootLevel = false)
@Entity
public class ComputedBean {

    @ComputedAttribute
    public String getTest() {
        return "test1";
    }

    @ComputedAttribute
    public String getTestWithScope(com.paiondata.elide.core.RequestScope requestScope) {
        return "test2";
    }

    @ComputedAttribute
    public String getTestWithSecurityScope(RequestScope requestScope) {
        return "test3";
    }

    public String getNonComputedWithScope(com.paiondata.elide.core.RequestScope requestScope) {
        return "should not run!";
    }
}
