/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.datastores.jpa.usertypes.JsonType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Include
@Audited // Ensure envers does not cause any issues
@Data
public class Person {
    @Id
    private long id;

    private String name;

    //For testing Convert annotation
    @Column(name = "address", columnDefinition = "TEXT")
    @Convert(converter = AddressFragment.Converter.class)
    private AddressFragment address;

    //For testing Type annotation
    @Column(name = "addressAlternate", columnDefinition = "TEXT")
    @Type(value = JsonType.class, parameters = {
            @Parameter(name = "class", value = "example.AddressFragment")
    })
    private AddressFragment alternateAddress;
}
