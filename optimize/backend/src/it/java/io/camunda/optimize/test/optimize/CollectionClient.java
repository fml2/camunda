/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.test.optimize;

import static io.camunda.optimize.dto.optimize.DefinitionType.DECISION;
import static io.camunda.optimize.dto.optimize.DefinitionType.PROCESS;
import static io.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import io.camunda.optimize.OptimizeRequestExecutor;
import io.camunda.optimize.dto.optimize.DefinitionType;
import io.camunda.optimize.dto.optimize.query.IdResponseDto;
import io.camunda.optimize.dto.optimize.query.alert.AlertDefinitionDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionRestDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionRoleRequestDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionRoleResponseDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionRoleUpdateRequestDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryDto;
import io.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryUpdateDto;
import io.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionRequestDto;
import io.camunda.optimize.dto.optimize.query.entity.EntityResponseDto;
import io.camunda.optimize.dto.optimize.rest.AuthorizedCollectionDefinitionRestDto;
import io.camunda.optimize.dto.optimize.rest.AuthorizedReportDefinitionResponseDto;
import io.camunda.optimize.dto.optimize.rest.ConflictResponseDto;
import io.camunda.optimize.dto.optimize.rest.collection.CollectionScopeEntryResponseDto;
import io.camunda.optimize.dto.optimize.rest.sorting.EntitySorter;
import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.function.Supplier;

public class CollectionClient {

  public static final String DEFAULT_DEFINITION_KEY = "defaultScopeDefinitionKey";
  public static final String DEFAULT_TENANT = null;
  public static final List<String> DEFAULT_TENANTS = singletonList(DEFAULT_TENANT);
  public static final String PRIVATE_COLLECTION_ID = null;

  private final Supplier<OptimizeRequestExecutor> requestExecutorSupplier;

  public CollectionClient(final Supplier<OptimizeRequestExecutor> requestExecutorSupplier) {
    this.requestExecutorSupplier = requestExecutorSupplier;
  }

  public String createNewCollectionForAllDefinitionTypes() {
    final String collectionId = createNewCollectionWithDefaultScope(PROCESS);
    createScopeWithTenants(collectionId, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS, DECISION);
    return collectionId;
  }

  public String createNewCollectionWithDefaultProcessScope() {
    return createNewCollectionWithDefaultScope(PROCESS);
  }

  public String createNewCollectionWithDefaultDecisionScope() {
    return createNewCollectionWithDefaultScope(DECISION);
  }

  public String createNewCollectionWithDefaultScope(final DefinitionType definitionType) {
    final String collectionId = createNewCollection();
    createScopeWithTenants(collectionId, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS, definitionType);
    return collectionId;
  }

  public String createNewCollectionWithScope(
      final String username,
      final String password,
      final DefinitionType definitionType,
      final String definitionKey,
      final List<String> tenantIds) {
    final String collectionId = createNewCollection(username, password);
    createScopeWithTenants(collectionId, definitionKey, tenantIds, definitionType);
    return collectionId;
  }

  public String createNewCollectionWithProcessScope(
      final ProcessInstanceEngineDto instanceEngineDto) {
    final String collectionId = createNewCollection();
    createScopeWithTenants(
        collectionId,
        instanceEngineDto.getProcessDefinitionKey(),
        singletonList(instanceEngineDto.getTenantId()),
        PROCESS);
    return collectionId;
  }

  // TODO recreate C8 IT equivalent helpers of these with #13337
  //  public String createNewCollectionWithProcessScope(
  //      final ProcessDefinitionEngineDto definitionEngineDto) {
  //    final String collectionId = createNewCollection();
  //    createScopeWithTenants(
  //        collectionId,
  //        definitionEngineDto.getKey(),
  //        singletonList(definitionEngineDto.getTenantId().orElse(null)),
  //        PROCESS);
  //    return collectionId;
  //  }

  public String createNewCollection() {
    return getRequestExecutor()
        .buildCreateCollectionRequest()
        .execute(IdResponseDto.class, Response.Status.OK.getStatusCode())
        .getId();
  }

  public String createNewCollection(final String user, final String password) {
    return getRequestExecutor()
        .withUserAuthentication(user, password)
        .buildCreateCollectionRequest()
        .execute(IdResponseDto.class, Response.Status.OK.getStatusCode())
        .getId();
  }

  public void updateCollection(
      final String collectionId, final PartialCollectionDefinitionRequestDto updatedCollection) {
    updateCollectionAsUser(collectionId, updatedCollection, DEFAULT_USERNAME, DEFAULT_USERNAME);
  }

  public void updateCollectionAsUser(
      final String collectionId,
      final PartialCollectionDefinitionRequestDto updatedCollection,
      final String username,
      final String password) {
    getRequestExecutor()
        .buildUpdatePartialCollectionRequest(collectionId, updatedCollection)
        .withUserAuthentication(username, password)
        .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public CollectionDefinitionRestDto getCollectionById(final String collectionId) {
    return getRequestExecutor()
        .buildGetCollectionRequest(collectionId)
        .execute(CollectionDefinitionRestDto.class, Response.Status.OK.getStatusCode());
  }

  public AuthorizedCollectionDefinitionRestDto getAuthorizedCollectionById(
      final String collectionId) {
    return getAuthorizedCollectionById(collectionId, DEFAULT_USERNAME, DEFAULT_USERNAME);
  }

  public AuthorizedCollectionDefinitionRestDto getAuthorizedCollectionById(
      final String collectionId, final String username, final String password) {
    return getRequestExecutor()
        .buildGetCollectionRequest(collectionId)
        .withUserAuthentication(username, password)
        .execute(AuthorizedCollectionDefinitionRestDto.class, Response.Status.OK.getStatusCode());
  }

  public List<AuthorizedReportDefinitionResponseDto> getReportsForCollection(
      final String collectionId) {
    return getRequestExecutor()
        .buildGetReportsForCollectionRequest(collectionId)
        .executeAndReturnList(AuthorizedReportDefinitionResponseDto.class, 200);
  }

  public List<AuthorizedReportDefinitionResponseDto> getReportsForCollectionAsUser(
      final String collectionId, final String username, final String password) {
    return getRequestExecutor()
        .buildGetReportsForCollectionRequest(collectionId)
        .withUserAuthentication(username, password)
        .executeAndReturnList(AuthorizedReportDefinitionResponseDto.class, 200);
  }

  public List<AlertDefinitionDto> getAlertsForCollection(final String collectionId) {
    return getRequestExecutor()
        .buildGetAlertsForCollectionRequest(collectionId)
        .executeAndReturnList(AlertDefinitionDto.class, 200);
  }

  public List<EntityResponseDto> getEntitiesForCollection(final String collectionId) {
    return getEntitiesForCollection(collectionId, null);
  }

  public List<EntityResponseDto> getEntitiesForCollection(
      final String collectionId, final String user, final String pass) {
    return getEntitiesForCollection(collectionId, null, user, pass);
  }

  public List<EntityResponseDto> getEntitiesForCollection(
      final String collectionId, final EntitySorter entitySorter) {
    return getEntitiesForCollection(collectionId, entitySorter, DEFAULT_USERNAME, DEFAULT_USERNAME);
  }

  public List<EntityResponseDto> getEntitiesForCollection(
      final String collectionId,
      final EntitySorter entitySorter,
      final String username,
      final String password) {
    return getRequestExecutor()
        .buildGetCollectionEntitiesRequest(collectionId, entitySorter)
        .withUserAuthentication(username, password)
        .executeAndReturnList(EntityResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public void updateCollectionScopeAsKermit(
      final String collectionId, final String scopeEntryId, final List<String> tenants) {
    getRequestExecutor()
        .buildUpdateCollectionScopeEntryRequest(
            collectionId, scopeEntryId, new CollectionScopeEntryUpdateDto(tenants))
        .withUserAuthentication("kermit", "kermit")
        .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void createScopeForCollection(
      final String collectionId, final String definitionKey, final DefinitionType definitionType) {
    addScopeEntryToCollection(collectionId, createSimpleScopeEntry(definitionKey, definitionType));
  }

  public List<CollectionScopeEntryResponseDto> getCollectionScope(final String collectionId) {
    return getRequestExecutor()
        .buildGetScopeForCollectionRequest(collectionId)
        .execute(new TypeReference<>() {});
  }

  public List<CollectionScopeEntryResponseDto> getCollectionScopeForKermit(
      final String collectionId) {
    return getRequestExecutor()
        .buildGetScopeForCollectionRequest(collectionId)
        .withUserAuthentication("kermit", "kermit")
        .execute(new TypeReference<>() {});
  }

  public void createScopeWithTenants(
      final String collectionId,
      final String definitionKey,
      final List<String> tenants,
      final DefinitionType definitionType) {
    final CollectionScopeEntryDto scopeEntry =
        new CollectionScopeEntryDto(definitionType, definitionKey, tenants);
    addScopeEntryToCollection(collectionId, scopeEntry);
  }

  public Response deleteCollection(final String id) {
    return getRequestExecutor().buildDeleteCollectionRequest(id, true).execute();
  }

  public CollectionScopeEntryDto createSimpleScopeEntry(
      final String definitionKey, final DefinitionType definitionType) {
    return new CollectionScopeEntryDto(definitionType, definitionKey, DEFAULT_TENANTS);
  }

  public CollectionScopeEntryDto createSimpleScopeEntry(
      final String definitionKey,
      final DefinitionType definitionType,
      final List<String> tenantIds) {
    return new CollectionScopeEntryDto(definitionType, definitionKey, tenantIds);
  }

  public void addScopeEntryToCollection(
      final String collectionId,
      final String definitionKey,
      final DefinitionType definitionType,
      final List<String> tenantIds) {
    addScopeEntryToCollection(
        collectionId, createSimpleScopeEntry(definitionKey, definitionType, tenantIds));
  }

  public void addScopeEntryToCollection(
      final String collectionId, final CollectionScopeEntryDto entry) {
    addScopeEntriesToCollection(collectionId, singletonList(entry));
  }

  public void addScopeEntriesToCollection(
      final String collectionId, final List<CollectionScopeEntryDto> entries) {
    getRequestExecutor().buildAddScopeEntriesToCollectionRequest(collectionId, entries).execute();
  }

  public void addScopeEntryToCollectionAsUser(
      final String collectionId,
      final CollectionScopeEntryDto entry,
      final String user,
      final String password) {
    getRequestExecutor()
        .buildAddScopeEntriesToCollectionRequest(collectionId, singletonList(entry))
        .withUserAuthentication(user, password)
        .execute(IdResponseDto.class, Response.Status.NO_CONTENT.getStatusCode());
  }

  public void updateCollectionScopeEntry(
      final String collectionId,
      final CollectionScopeEntryUpdateDto scopeEntryUpdate,
      final String scopeEntryId) {
    final Response response =
        getRequestExecutor()
            .buildUpdateCollectionScopeEntryRequest(
                collectionId, scopeEntryId, scopeEntryUpdate, true)
            .execute();
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void deleteScopeEntry(
      final String collectionId, final CollectionScopeEntryDto scopeEntry, final Boolean force) {
    final Response response =
        getRequestExecutor()
            .buildDeleteScopeEntryFromCollectionRequest(collectionId, scopeEntry.getId(), force)
            .execute();
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void addRoleToCollectionAsUser(
      final String collectionId,
      final CollectionRoleRequestDto roleDto,
      final String username,
      final String password) {
    addRolesToCollectionAsUser(
        collectionId, new CollectionRoleRequestDto[] {roleDto}, username, password);
  }

  public void addRolesToCollection(
      final String collectionId, final CollectionRoleRequestDto... rolesToAdd) {
    addRolesToCollectionAsUser(collectionId, rolesToAdd, DEFAULT_USERNAME, DEFAULT_USERNAME);
  }

  private void addRolesToCollectionAsUser(
      final String collectionId,
      final CollectionRoleRequestDto[] rolesToAdd,
      final String username,
      final String password) {
    getRequestExecutor()
        .buildAddRolesToCollectionRequest(collectionId, rolesToAdd)
        .withUserAuthentication(username, password)
        .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void updateCollectionRoleAsUser(
      final String collectionId,
      final String roleId,
      final CollectionRoleUpdateRequestDto updateDto,
      final String username,
      final String password) {
    getRequestExecutor()
        .buildUpdateRoleToCollectionRequest(collectionId, roleId, updateDto)
        .withUserAuthentication(username, password)
        .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void deleteCollectionRoleAsUser(
      final String collectionId,
      final String roleId,
      final String username,
      final String password) {
    getRequestExecutor()
        .buildDeleteRoleToCollectionRequest(collectionId, roleId)
        .withUserAuthentication(username, password)
        .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public OptimizeRequestExecutor getAlertsRequest(
      final String userId, final String password, final String collectionId) {
    return getRequestExecutor()
        .buildGetAlertsForCollectionRequest(collectionId)
        .withUserAuthentication(userId, password);
  }

  public List<CollectionRoleResponseDto> getCollectionRolesAsUser(
      final String collectionId, final String username, final String password) {
    return getRequestExecutor()
        .buildGetRolesToCollectionRequest(collectionId)
        .withUserAuthentication(username, password)
        .executeAndReturnList(CollectionRoleResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public List<CollectionRoleResponseDto> getCollectionRoles(final String collectionId) {
    return getRequestExecutor()
        .buildGetRolesToCollectionRequest(collectionId)
        .executeAndReturnList(CollectionRoleResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public List<IdResponseDto> getCollectionRoleIdDtos(final String collectionId) {
    return getRequestExecutor()
        .buildGetRolesToCollectionRequest(collectionId)
        .executeAndReturnList(IdResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public CollectionDefinitionRestDto copyCollection(final String collectionId) {
    return copyCollection(collectionId, null);
  }

  public CollectionDefinitionRestDto copyCollection(
      final String collectionId, final String newName) {
    final OptimizeRequestExecutor executor =
        getRequestExecutor().buildCopyCollectionRequest(collectionId);
    if (newName != null) {
      executor.addSingleQueryParam("name", newName);
    }
    return executor.execute(CollectionDefinitionRestDto.class, Response.Status.OK.getStatusCode());
  }

  public ConflictResponseDto getScopeDeletionConflicts(
      final String collectionId, final String scopeEntryId) {
    return getRequestExecutor()
        .buildGetScopeDeletionConflictsRequest(collectionId, scopeEntryId)
        .execute(ConflictResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public boolean collectionScopesHaveDeleteConflict(
      final String collectionId, final List<String> collectionScopeIds) {
    return getRequestExecutor()
        .buildCheckScopeBulkDeletionConflictsRequest(collectionId, collectionScopeIds)
        .execute(Boolean.class, Response.Status.OK.getStatusCode());
  }

  public void assertCollectionIsDeleted(final String idToDelete) {
    getRequestExecutor()
        .buildGetCollectionRequest(idToDelete)
        .execute(Response.Status.NOT_FOUND.getStatusCode());
  }

  private OptimizeRequestExecutor getRequestExecutor() {
    return requestExecutorSupplier.get();
  }

  public static CollectionClientBuilder builder() {
    return new CollectionClientBuilder();
  }

  public static class CollectionClientBuilder {

    private Supplier<OptimizeRequestExecutor> requestExecutorSupplier;

    CollectionClientBuilder() {}

    public CollectionClientBuilder requestExecutorSupplier(
        final Supplier<OptimizeRequestExecutor> requestExecutorSupplier) {
      this.requestExecutorSupplier = requestExecutorSupplier;
      return this;
    }

    public CollectionClient build() {
      return new CollectionClient(requestExecutorSupplier);
    }

    @Override
    public String toString() {
      return "CollectionClient.CollectionClientBuilder(requestExecutorSupplier="
          + requestExecutorSupplier
          + ")";
    }
  }
}
