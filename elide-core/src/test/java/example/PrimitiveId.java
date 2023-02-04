/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;


@Include(name = "primitiveTypeId")
@Entity
@Data
public class PrimitiveId {

    @Id
    private long primitiveId;
}
