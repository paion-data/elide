/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.models.AsyncApi;
import com.paiondata.elide.async.models.AsyncApiResult;
import com.paiondata.elide.async.models.QueryStatus;
import com.paiondata.elide.async.operation.AsyncApiUpdateOperation;
import com.paiondata.elide.async.service.dao.AsyncApiDao;
import com.paiondata.elide.core.security.User;

import jakarta.inject.Inject;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service to execute Async queries.
 * It will schedule task to track long running queries and kills them.
 * It will also schedule task to update orphan query statuses after
 * host/app crash or restart.
 */
@Getter
@Slf4j
public class AsyncExecutorService {

    public static final int DEFAULT_THREAD_POOL_SIZE = 6;

    private final Elide elide;
    private final ExecutorService executor;
    private final ExecutorService updater;
    private final AsyncApiDao asyncApiDao;
    private final ThreadLocal<AsyncApiResultFuture> asyncResultFutureThreadLocal = new ThreadLocal<>();
    private final AsyncProviderService providers;

    /**
     * A Future with Synchronous Execution Complete Flag.
     */
    @Data
    private static class AsyncApiResultFuture {
        private Future<AsyncApiResult> asyncFuture;
        private boolean synchronousTimeout = false;
    }

    @Inject
    public AsyncExecutorService(Elide elide, ExecutorService executor, ExecutorService updater, AsyncApiDao asyncApiDao,
            AsyncProviderService asyncProviderService) {
        this.elide = elide;
        this.executor = executor;
        this.updater = updater;
        this.asyncApiDao = asyncApiDao;
        this.providers = asyncProviderService;
    }

    /**
     * Execute Query asynchronously.
     * @param queryObj Query Object
     * @param callable A Callabale implementation to execute in background.
     */
    public void executeQuery(AsyncApi queryObj, Callable<AsyncApiResult> callable) {
        AsyncApiResultFuture resultFuture = new AsyncApiResultFuture();
        try {
            Future<AsyncApiResult> asyncExecuteFuture = executor.submit(callable);
            resultFuture.setAsyncFuture(asyncExecuteFuture);
            queryObj.setStatus(QueryStatus.PROCESSING);
            AsyncApiResult queryResultObj = asyncExecuteFuture.get(queryObj.getAsyncAfterSeconds(), TimeUnit.SECONDS);
            queryObj.setResult(queryResultObj);
            queryObj.setStatus(QueryStatus.COMPLETE);
            queryObj.setUpdatedOn(new Date());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("InterruptedException: {}", e.toString());
            queryObj.setStatus(QueryStatus.FAILURE);
        } catch (ExecutionException e) {
            log.error("ExecutionException: {}", e.toString());
            queryObj.setStatus(QueryStatus.FAILURE);
        } catch (TimeoutException e) {
            log.error("TimeoutException: {}", e.toString());
            resultFuture.setSynchronousTimeout(true);
        } catch (Exception e) {
            log.error("Exception: {}", e.toString());
            queryObj.setStatus(QueryStatus.FAILURE);
        } finally {
            asyncResultFutureThreadLocal.set(resultFuture);
        }

    }
    /**
     * Complete Query asynchronously.
     * @param query AsyncQuery
     * @param user User
     * @param apiVersion API Version
     */
    public void completeQuery(AsyncApi query, User user, String apiVersion) {
        AsyncApiResultFuture asyncApiResultFuture = asyncResultFutureThreadLocal.get();
        if (asyncApiResultFuture.isSynchronousTimeout()) {
            log.debug("Task has not completed");
            updater.execute(new AsyncApiUpdateOperation(elide, asyncApiResultFuture.getAsyncFuture(), query,
                    asyncApiDao));
            asyncResultFutureThreadLocal.remove();
        } else {
            log.debug("Task has completed");
        }
    }
}
