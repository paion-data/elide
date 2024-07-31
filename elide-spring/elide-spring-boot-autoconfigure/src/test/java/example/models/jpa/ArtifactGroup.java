/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.graphql.subscriptions.annotations.Subscription;
import com.paiondata.elide.graphql.subscriptions.annotations.SubscriptionField;
import example.checks.AdminCheck;
import example.hooks.ArtifactGroupHook;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Include(name = "group")
@Entity
@Data
@Subscription
@LifeCycleHookBinding(operation = LifeCycleHookBinding.Operation.UPDATE, phase = LifeCycleHookBinding.TransactionPhase.PREFLUSH, hook = ArtifactGroupHook.class)
public class ArtifactGroup {
    @Id
    private String name = "";

    @SubscriptionField
    private String commonName = "";

    private String description = "";

    @CreatePermission(expression = AdminCheck.USER_IS_ADMIN)
    @UpdatePermission(expression = AdminCheck.USER_IS_ADMIN)
    @SubscriptionField
    private boolean deprecated = false;

    @SubscriptionField
    @OneToMany(mappedBy = "group")
    private List<ArtifactProduct> products = new ArrayList<>();
}
