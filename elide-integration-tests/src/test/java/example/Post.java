/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;

import jakarta.persistence.Entity;

/**
 * Post test bean.
 */
@CreatePermission(expression = "Prefab.Role.All")
@ReadPermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.All OR Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.All OR Prefab.Role.None")
@Include(name = "post") // optional here because class has this name
// Hibernate
@Entity
public class Post extends BaseId {
    private String title;
    private int created;

    @ReadPermission(expression = "Prefab.Role.None") public transient boolean init = false;

    public void doInit() {
        init = true;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCreated() {
        return this.created;
    }

    public void setCreated(int created) {
        this.created = created;
    }
}
