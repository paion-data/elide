/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package hooks;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.lifecycle.LifeCycleHook;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.graphql.GraphQLEndpointTest;

import graphqlEndpointTestModels.Book;

import java.util.Optional;

/**
 * Test pre-commit hook for updates to Book.title.
 */
public class BookUpdatePreCommitHook implements LifeCycleHook<Book> {
    @Override
    public void execute(LifeCycleHookBinding.Operation operation, LifeCycleHookBinding.TransactionPhase phase,
                          Book elideEntity, RequestScope requestScope, Optional<ChangeSpec> changes) {
        GraphQLEndpointTest.User user = (GraphQLEndpointTest.User) requestScope.getUser().getPrincipal();
        user.appendLog("On Title Update Pre Commit\n");
    }
}
