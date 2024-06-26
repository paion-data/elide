/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.Audit;
import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

import java.util.List;

@Entity
@Include
@ReadPermission(expression = "Prefab.Role.All")
@CreatePermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.All")
@DeletePermission(expression = "Prefab.Role.All")
public class AuditEntityInverse extends BaseId {
    private List<AuditEntity> entities;

    @ManyToMany
    @Audit(action = Audit.Action.UPDATE,
            logStatement = "Inverse entities: {0}",
            logExpressions = "${auditEntityInverse.entities}")
    public List<AuditEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<AuditEntity> entities) {
        this.entities = entities;
    }

    @Override
    public String toString() {
        return "AuditEntityInverse{"
                + "id=" + id
                + ", entities=" + entities
                + '}';
    }
}
