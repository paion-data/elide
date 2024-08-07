/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.parser.state;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.RelationshipType;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.exceptions.InternalServerErrorException;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.exceptions.InvalidObjectIdentifierException;
import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.exceptions.UnknownEntityException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Pagination;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.jsonapi.JsonApiMapper;
import com.paiondata.elide.jsonapi.JsonApiRequestScope;
import com.paiondata.elide.jsonapi.document.processors.DocumentProcessor;
import com.paiondata.elide.jsonapi.document.processors.IncludedProcessor;
import com.paiondata.elide.jsonapi.document.processors.PopulateMetaProcessor;
import com.paiondata.elide.jsonapi.models.Data;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.models.Meta;
import com.paiondata.elide.jsonapi.models.Relationship;
import com.paiondata.elide.jsonapi.models.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.ToString;
import reactor.core.publisher.Flux;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Collection State.
 */
@ToString
public class CollectionTerminalState extends BaseState {
    private final Optional<PersistentResource<?>> parent;
    private final Optional<String> relationName;
    private final Type<?> entityClass;
    private PersistentResource<?> newObject;
    private final EntityProjection parentProjection;

    public CollectionTerminalState(Type<?> entityClass, Optional<PersistentResource<?>> parent,
                                   Optional<String> relationName, EntityProjection projection) {
        this.parentProjection = projection;
        this.parent = parent;
        this.relationName = relationName;
        this.entityClass = entityClass;
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handleGet(StateContext state) {
        JsonApiDocument jsonApiDocument = new JsonApiDocument();
        RequestScope requestScope = state.getRequestScope();
        Map<String, List<String>> queryParams = requestScope.getRoute().getParameters();

        LinkedHashSet<PersistentResource> collection =
                getResourceCollection(requestScope).collect(Collectors.toCollection(LinkedHashSet::new)).block();

        // Set data
        jsonApiDocument.setData(getData(collection, requestScope.getDictionary()));

        // Run include processor
        DocumentProcessor includedProcessor = new IncludedProcessor();
        includedProcessor.execute(jsonApiDocument, requestScope, collection, queryParams);

        Pagination pagination = parentProjection.getPagination();
        if (parent.isPresent()) {
            pagination = parentProjection.getRelationship(relationName.orElseThrow(IllegalStateException::new))
                    .get().getProjection().getPagination();
        }

        // Add pagination meta data
        if (!pagination.isDefaultInstance()) {

            Map<String, Number> pageMetaData = new HashMap<>();
            pageMetaData.put("number", (pagination.getOffset() / pagination.getLimit()) + 1);
            pageMetaData.put("limit", pagination.getLimit());

            // Get total records if it has been requested and add to the page meta data
            if (pagination.returnPageTotals()) {
                Long totalRecords = pagination.getPageTotals();
                pageMetaData.put("totalPages", totalRecords / pagination.getLimit()
                        + ((totalRecords % pagination.getLimit()) > 0 ? 1 : 0));
                pageMetaData.put("totalRecords", totalRecords);
            }

            Map<String, Object> allMetaData = new HashMap<>();
            allMetaData.put("page", pageMetaData);

            Meta meta = new Meta(allMetaData);
            jsonApiDocument.setMeta(meta);
        }

        PopulateMetaProcessor metaProcessor = new PopulateMetaProcessor();
        metaProcessor.execute(jsonApiDocument, requestScope, collection, queryParams);

        return () -> Pair.of(HttpStatus.SC_OK, jsonApiDocument);
    }

    @Override
    public Supplier<Pair<Integer, JsonApiDocument>> handlePost(StateContext state) {
        JsonApiRequestScope requestScope = state.getRequestScope();

        newObject = createObject(requestScope);
        parent.ifPresent(persistentResource -> persistentResource.addRelation(relationName.get(), newObject));
        return () -> {
            JsonApiDocument returnDoc = new JsonApiDocument();
            returnDoc.setData(new Data<>(newObject.toResource()));

            PopulateMetaProcessor metaProcessor = new PopulateMetaProcessor();
            metaProcessor.execute(returnDoc, requestScope, newObject, requestScope.getRoute().getParameters());

            return Pair.of(HttpStatus.SC_CREATED, returnDoc);
        };
    }

    private Flux<PersistentResource> getResourceCollection(RequestScope requestScope) {
        final Flux<PersistentResource> collection;
        // TODO: In case of join filters, apply pagination after getting records
        // instead of passing it to the datastore

        if (parent.isPresent()) {
            collection = parent.get().getRelationCheckedFiltered(
                    parentProjection.getRelationship(relationName.orElseThrow(IllegalStateException::new))
                            .orElseThrow(IllegalStateException::new));
        } else {
            collection = PersistentResource.loadRecords(
                parentProjection,
                Collections.emptyList(), //Empty list of IDs
                requestScope);
        }

        return collection;
    }

    private Data getData(Set<PersistentResource> collection, EntityDictionary dictionary) {
        Preconditions.checkNotNull(collection);
        List<Resource> resources = collection.stream().map(PersistentResource::toResource).collect(Collectors.toList());

        if (parent.isPresent()) {
            Type<?> parentClass = parent.get().getResourceType();
            String relationshipName = relationName.orElseThrow(IllegalStateException::new);
            RelationshipType type = dictionary.getRelationshipType(parentClass, relationshipName);

            return new Data<>(resources, type);
        }
        return new Data<>(resources);
    }

    private PersistentResource createObject(JsonApiRequestScope requestScope)
        throws ForbiddenAccessException, InvalidObjectIdentifierException {
        JsonApiDocument doc = requestScope.getJsonApiDocument();
        JsonApiMapper mapper = requestScope.getMapper();

        if (doc.getData() == null) {
            throw new InvalidEntityBodyException("Invalid JSON-API document: " + doc);
        }

        Data<Resource> data = doc.getData();
        Collection<Resource> resources = data.get();

        Resource resource = (resources.size() == 1) ? IterableUtils.first(resources) : null;
        if (resource == null) {
            try {
                throw new InvalidEntityBodyException(mapper.writeJsonApiDocument(doc));
            } catch (JsonProcessingException e) {
                throw new InternalServerErrorException(e);
            }
        }

        String id = resource.getId();

        Type<?> newObjectClass = requestScope.getDictionary().getEntityClass(resource.getType(),
                requestScope.getRoute().getApiVersion());

        if (newObjectClass == null) {
            throw new UnknownEntityException("Entity " + resource.getType() + " not found");
        }
        if (!entityClass.isAssignableFrom(newObjectClass)) {
            throw new InvalidValueException("Cannot assign value of type: " + resource.getType()
                    + " to type: " + entityClass);
        }

        PersistentResource pResource = PersistentResource.createObject(
                parent.orElse(null),
                relationName.orElse(null),
                newObjectClass,
                requestScope, Optional.ofNullable(id));

        Map<String, Object> attributes = resource.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String fieldName = entry.getKey();
                Object val = entry.getValue();
                pResource.updateAttribute(fieldName, val);
            }
        }

        Map<String, Relationship> relationships = resource.getRelationships();
        if (relationships != null) {
            for (Map.Entry<String, Relationship> entry : relationships.entrySet()) {
                String fieldName = entry.getKey();
                Relationship relationship = entry.getValue();
                Set<PersistentResource> resourceSet = (relationship == null)
                                                    ? null
                                                    : relationship.toPersistentResources(requestScope);
                pResource.updateRelation(fieldName, resourceSet);
            }
        }

        return pResource;
    }
}
