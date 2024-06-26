/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.example.beans;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.Exclude;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Hibernate-managed user.
 */
@Include
@Entity
@CreatePermission(expression = "Prefab.Role.All")
@ReadPermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.All")
@DeletePermission(expression = "Prefab.Role.All")
public class HibernateUser {
    private Long id;
    private String firstName;
    private Integer specialActionId;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Exclude // Hide this from elide: this is the internal pointer for finding this value in hbase
    public Integer getSpecialActionId() {
        return specialActionId;
    }

    public void setSpecialActionId(Integer specialActionId) {
        this.specialActionId = specialActionId;
    }
}
