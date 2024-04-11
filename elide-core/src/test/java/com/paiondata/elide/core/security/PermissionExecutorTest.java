/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.annotation.DeletePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.checks.OperationCheck;
import com.paiondata.elide.core.security.checks.UserCheck;
import com.paiondata.elide.core.security.permissions.ExpressionResult;
import com.paiondata.elide.core.type.ClassType;
import example.TestCheckMappings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class PermissionExecutorTest {

    interface SampleOperationModel {
        default boolean test() {
            return true;
        }
    }

    @Test
    public void testSuccessfulOperationCheck() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "sampleOperation")
        class Model implements SampleOperationModel {
        }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.PASS,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));

        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testFailOperationCheckAll() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "sampleOperation AND Prefab.Role.None")
        class Model implements SampleOperationModel { }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
    }

    @Test
    public void testFailOperationCheckDeferred() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "sampleOperation")
        class Model implements SampleOperationModel {

            @Override
            public boolean test() {
                return false;
            }
        }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();

        // Because the object is newly created, the check is DEFERRED.
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));

        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testSuccessfulCommitChecks() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "sampleOperation")
        class Model implements SampleOperationModel { }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();

        // Because the object is newly created, the check is DEFERRED.
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));

        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testSuccessfulRunAtCommitCheck() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "sampleCommit")
        class Model implements SampleOperationModel { }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();

        // Because the check is runAtCommit, the check is DEFERRED.
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));

        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testFailRunAtCommitCheck() throws Exception {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "sampleCommit")
        class Model implements SampleOperationModel {
            @Override
            public boolean test() {
                return false;
            }
        }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();

        // Because the check is runAtCommit, the check is DEFERRED.
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));

        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testReadFieldAwareSuccessAllAnyField() {
        SampleBean sampleBean = new SampleBean();
        sampleBean.id = 1L;
        com.paiondata.elide.core.PersistentResource resource = newResource(sampleBean, SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.PASS,
                requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessFailureAnyField() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAll() {
        SampleBean sampleBean = new SampleBean();
        sampleBean.id = 1L;
        com.paiondata.elide.core.PersistentResource resource = newResource(sampleBean, SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.PASS,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "allVisible"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAllSpecificField() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "allVisible"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAllNoOverride() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "defaultHidden"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAll() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "cannotSeeMe"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareSuccessAny() {
        SampleBean sampleBean = new SampleBean();
        sampleBean.id = 1L;
        com.paiondata.elide.core.PersistentResource resource = newResource(sampleBean, SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.PASS,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), ReadPermission.class, "mayFailInCommit"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFieldAwareFailureAny() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "mayFailInCommit"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareSuccessAll() {
        SampleBean sampleBean = new SampleBean();
        sampleBean.id = 1L;
        com.paiondata.elide.core.PersistentResource resource = newResource(sampleBean, SampleBean.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "allVisible"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareFailureAll() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "allVisible");
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testUpdateFieldAwareSuccessAny() {
        SampleBean sampleBean = new SampleBean();
        sampleBean.id = 1L;
        com.paiondata.elide.core.PersistentResource resource = newResource(sampleBean, SampleBean.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "mayFailInCommit"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testUpdateFieldAwareFailureAny() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "mayFailInCommit");
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testReadFieldAwareEntireOpenBean() {
        OpenBean openBean = new OpenBean();
        openBean.id = 1L;
        com.paiondata.elide.core.PersistentResource resource = newResource(openBean, OpenBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        Assertions.assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class, "open"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testReadFailureFieldAwareOpenBean() {
        com.paiondata.elide.core.PersistentResource resource = newResource(OpenBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkSpecificFieldPermissions(
                        resource, null, ReadPermission.class, "openAll"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testPassAnyFieldAwareFailOperationSuccessCommit() {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "Prefab.Role.None AND passingOp")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "Prefab.Role.None OR passingOp")
            public String field = "some data";
        }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testFailAllFieldAwareSuccessOperationFailCommit() {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "Prefab.Role.None")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "Prefab.Role.All AND FailOp")
            public String field = "some data";
        }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testPassAnySpecificFieldAwareFailOperationSuccessCommit() {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "Prefab.Role.None AND passingOp")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "Prefab.Role.None OR passingOp")
            public String field = "some data";
        }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "field"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testFailAllSpecificFieldAwareSuccessOperationFailCommit() {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "Prefab.Role.All")
        class Model {
            @Id
            public Long id;

            @UpdatePermission(expression = "Prefab.Role.All AND FailOp")
            public String field = "some data";
        }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "field"));
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testBadInstance() {
        @Entity
        @Include(rootLevel = false)
        @UpdatePermission(expression = "privatePermission")
        class Model { }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                IllegalArgumentException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testSpecificFieldOveriddenOperationCheckSucceed() {
        com.paiondata.elide.core.PersistentResource resource = newResource(CheckedEntity.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        // Should succeed in operation check despite the commit check failure
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, UpdatePermission.class, "hello"));
        requestScope.getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testSpecificFieldCommitCheckFailByOveriddenField() {
        com.paiondata.elide.core.PersistentResource resource = newResource(CheckedEntity.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.DEFERRED,
                requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, new ChangeSpec(null, null, null, null), UpdatePermission.class, "hello"));
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testReadCheckExpressionForNewlyCreatedObject() {
        @Entity
        @Include(rootLevel = false)
        @ReadPermission(expression = "FailOp")
        class Model { }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        requestScope.getDictionary().bindEntity(Model.class);
        Assertions.assertEquals(ExpressionResult.DEFERRED, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testDeleteCheckExpressionForNewlyCreatedObject() {
        @Entity
        @Include(rootLevel = false)
        @DeletePermission(expression = "FailOp")
        class Model { }

        com.paiondata.elide.core.PersistentResource resource = newResource(new Model(), Model.class, true);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        requestScope.getDictionary().bindEntity(Model.class);
        Assertions.assertEquals(ExpressionResult.DEFERRED, requestScope.getPermissionExecutor().checkPermission(DeletePermission.class, resource));
        assertThrows(ForbiddenAccessException.class, () -> requestScope.getPermissionExecutor().executeCommitChecks());
    }

    @Test
    public void testCache() {
        com.paiondata.elide.core.PersistentResource resource = newResource(AnnotationOnlyRecord.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        Assertions.assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
        Assertions.assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource));
    }


    @Test
    public void testNoCache() {
        com.paiondata.elide.core.PersistentResource resource = newResource(AnnotationOnlyRecord.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));
    }

    @Test
    public void testUserCheckCache() {
        com.paiondata.elide.core.PersistentResource resource = newResource(UserCheckCacheRecord.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        // This should cache for updates, reads, etc.
        Assertions.assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));
        Assertions.assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(UpdatePermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));
        Assertions.assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));
        Assertions.assertEquals(ExpressionResult.PASS, requestScope.getPermissionExecutor().checkPermission(ReadPermission.class, resource, com.paiondata.elide.core.PersistentResource.ALL_FIELDS));
    }

    @Test
    public void testUserCheckOnFieldSuccess() {
        OpenBean openBean = new OpenBean();
        openBean.id = 1L;
        com.paiondata.elide.core.PersistentResource resource = newResource(OpenBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        ExpressionResult result = requestScope.getPermissionExecutor().checkUserPermissions(ClassType.of(OpenBean.class),
                ReadPermission.class,
                "open");

        assertEquals(ExpressionResult.PASS, result);
    }

    @Test
    public void testUserCheckOnFieldFailure() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();
        assertThrows(
                ForbiddenAccessException.class,
                () -> requestScope.getPermissionExecutor().checkUserPermissions(ClassType.of(SampleBean.class),
                ReadPermission.class,
                "cannotSeeMe"));
    }

    @Test
    public void testUserCheckOnFieldDeferred() {
        com.paiondata.elide.core.PersistentResource resource = newResource(SampleBean.class, false);
        com.paiondata.elide.core.RequestScope requestScope = resource.getRequestScope();

        ExpressionResult result = requestScope.getPermissionExecutor().checkUserPermissions(ClassType.of(SampleBean.class),
                ReadPermission.class,
                "allVisible");

        assertEquals(ExpressionResult.DEFERRED, result);
    }

    public <T> com.paiondata.elide.core.PersistentResource<T> newResource(T obj, Class<T> cls, boolean markNew) {
        EntityDictionary dictionary = EntityDictionary.builder().checks(TestCheckMappings.MAPPINGS).build();
        dictionary.bindEntity(cls);
        Route route = Route.builder().apiVersion(EntityDictionary.NO_VERSION).build();
        com.paiondata.elide.core.RequestScope requestScope = RequestScope.builder().route(route).requestId(UUID.randomUUID())
                .elideSettings(getElideSettings(dictionary)).build();
        com.paiondata.elide.core.PersistentResource resource = new com.paiondata.elide.core.PersistentResource<>(obj, requestScope.getUUIDFor(obj), requestScope);
        if (markNew) {
            requestScope.getNewPersistentResources().add(resource);
        }
        return resource;
    }

    public <T> PersistentResource<T> newResource(Class<T> cls, boolean markNew) {
        try {
            T obj = cls.newInstance();
            return newResource(obj, cls, markNew);
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    private ElideSettings getElideSettings(EntityDictionary dictionary) {
        return ElideSettings.builder().dataStore(null)
                    .entityDictionary(dictionary)
                    .build();
    }

    public static final class SampleOperationCheck extends OperationCheck<SampleOperationModel> {
        @Override
        public boolean ok(SampleOperationModel model, com.paiondata.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return model.test();
        }
    }


    public static final class SampleOperationCheckInverse extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.paiondata.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return !changeSpec.isPresent();
        }
    }

    public static final class SampleCommitCheck extends OperationCheck<SampleOperationModel> {
        @Override
        public boolean runAtCommit() {
            return true;
        }

        @Override
        public boolean ok(SampleOperationModel model, com.paiondata.elide.core.security.RequestScope requestScope,
                Optional<ChangeSpec> changeSpec) {
            return model.test();
        }
    }

    @ReadPermission(expression = "Prefab.Role.None")
    @UpdatePermission(expression = "Prefab.Role.None")
    @Include(rootLevel = false)
    @Entity
    public static final class SampleBean implements SampleOperationModel {
        @Id
        public Long id;

        @Override
        public boolean test() {
            return id != null && id == 1L;
        }

        @ReadPermission(expression = "Prefab.Role.All AND sampleOperation")
        @UpdatePermission(expression = "Prefab.Role.All AND sampleOperation")
        public String allVisible = "You should see me!";

        public String defaultHidden = "I'm invisible. muwahaha...";

        @ReadPermission(expression = "Prefab.Role.All AND Prefab.Role.None")
        @UpdatePermission(expression = "Prefab.Role.All AND Prefab.Role.None")
        public String cannotSeeMe = "hidden";

        @ReadPermission(expression = "sampleOperation")
        @UpdatePermission(expression =  "sampleOperation OR Prefab.Role.None")
        public String mayFailInCommit = "aw :(";
    }

    @ReadPermission(expression = "Prefab.Role.All")
    @UpdatePermission(expression = "Prefab.Role.All")
    @Include(rootLevel = false)
    @Entity
    public static final class OpenBean implements SampleOperationModel {
        @Id
        public Long id;

        public String open;

        @Override
        public boolean test() {
            return id != null && id == 1L;
        }

        @ReadPermission(expression = "Prefab.Role.All AND sampleOperation")
        @UpdatePermission(expression = "Prefab.Role.All AND sampleOperation")
        public String openAll = "all";

        @ReadPermission(expression = "Prefab.Role.None OR sampleOperation")
        @UpdatePermission(expression = "Prefab.Role.None OR sampleOperation")
        public String openAny = "all";
    }

    @Entity
    @Include(rootLevel = false)
    @UpdatePermission(expression = "sampleOperation")
    public static final class CheckedEntity {
        @Id
        public Long id;

        @UpdatePermission(expression = "sampleOperationInverse")
        public String hello;
    }

    /* Cache testing */

    public static class ShouldCache extends OperationCheck<Object> {
        private static AtomicBoolean hasRun = new AtomicBoolean(false);
        @Override
        public boolean ok(Object object, com.paiondata.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return !hasRun.getAndSet(true);
        }
    }

    public static final class PassingOperationCheck extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.paiondata.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return true;
        }
    }

    public static final class FailingOperationCheck extends OperationCheck<Object> {
        @Override
        public boolean ok(Object object, com.paiondata.elide.core.security.RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return false;
        }
    }

    @Entity
    @Include(rootLevel = false)
    @ReadPermission(expression = "shouldCache")
    @UpdatePermission(expression = "shouldCache")
    public static class AnnotationOnlyRecord {
    }

    /* UserCheck cache testing */

    public static class UserCheckTest extends UserCheck {
        public AtomicBoolean hasBeenCalled = new AtomicBoolean(false);
        @Override
        public boolean ok(User user) {
            return !hasBeenCalled.getAndSet(true);
        }
    }

    @Entity
    @Include(rootLevel = false)
    @ReadPermission(expression = "peUserCheck")
    @UpdatePermission(expression = "peUserCheck")
    public static class UserCheckCacheRecord {
    }
}
