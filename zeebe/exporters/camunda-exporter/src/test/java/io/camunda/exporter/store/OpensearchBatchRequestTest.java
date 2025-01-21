/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.exporter.entities.TestExporterEntity;
import io.camunda.exporter.exceptions.PersistenceException;
import io.camunda.exporter.utils.OpensearchScriptBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkRequest.Builder;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;

class OpensearchBatchRequestTest {

  private static final String ID = "id";
  private static final String INDEX = "index";
  private OpensearchBatchRequest batchRequest;
  private OpenSearchClient osClient;
  private Builder requestBuilder;
  private OpensearchScriptBuilder scriptBuilder;

  @BeforeEach
  void setUp() throws IOException {
    osClient = mock(OpenSearchClient.class);
    requestBuilder = new Builder();
    scriptBuilder = mock(OpensearchScriptBuilder.class);
    batchRequest = new OpensearchBatchRequest(osClient, requestBuilder, scriptBuilder);
    final BulkResponse bulkResponse = mock(BulkResponse.class);
    when(osClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);
  }

  @Test
  void shouldAddABulkOperationWithSpecifiedIndexAndEntity()
      throws PersistenceException, IOException {
    // given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);

    // When
    batchRequest.add(INDEX, entity);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isIndex()).isTrue();
    final IndexOperation<TestExporterEntity> index = bulkOperation.index();
    assertThat(index.index()).isEqualTo(INDEX);
    assertThat(index.id()).isEqualTo(ID);
    assertThat(index.document()).isEqualTo(entity);
  }

  @Test
  void shouldAddWithRouting() throws IOException, PersistenceException {
    // given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);
    final String routing = "routing";

    // When
    batchRequest.addWithRouting(INDEX, entity, routing);

    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isIndex()).isTrue();

    final IndexOperation<TestExporterEntity> index = bulkOperation.index();
    assertThat(index.index()).isEqualTo(INDEX);
    assertThat(index.id()).isEqualTo(ID);
    assertThat(index.routing()).isEqualTo(routing);
    assertThat(index.document()).isEqualTo(entity);
  }

  @Test
  void shouldUpsertEntityWithUpdatedFields() throws IOException, PersistenceException {
    // given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);
    final Map<String, Object> updateFields = Map.of("id", "id2");

    // When
    batchRequest.upsert(INDEX, ID, entity, updateFields);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isUpdate()).isTrue();

    final var update = bulkOperation.update();
    assertThat(update.index()).isEqualTo(INDEX);
    assertThat(update.id()).isEqualTo(ID);
    assertThat(update.id()).isEqualTo(ID);
  }

  @Test
  void shouldUpsertWithRouting() throws PersistenceException, IOException {
    // given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);
    final Map<String, Object> updateFields = Map.of("id", "id2");
    final String routing = "routing";

    // When
    batchRequest.upsertWithRouting(INDEX, ID, entity, updateFields, routing);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isUpdate()).isTrue();

    final var update = bulkOperation.update();
    assertThat(update.index()).isEqualTo(INDEX);
    assertThat(update.id()).isEqualTo(ID);
    assertThat(update.routing()).isEqualTo(routing);
  }

  @Test
  void shouldUpsertWithScript() throws PersistenceException, IOException {
    // given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);
    final String script = "script";
    final Map<String, Object> params = Map.of("id", "id2");

    final Script scriptWithParameters = mock(Script.class);
    when(scriptBuilder.getScriptWithParameters(script, params)).thenReturn(scriptWithParameters);

    // When
    batchRequest.upsertWithScript(INDEX, ID, entity, script, params);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isUpdate()).isTrue();

    final var update = bulkOperation.update();
    assertThat(update.index()).isEqualTo(INDEX);
    assertThat(update.id()).isEqualTo(ID);
  }

  @Test
  void shouldUpsertWithScriptAndRouting() throws PersistenceException, IOException {
    // given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);
    final String script = "script";
    final Map<String, Object> params = Map.of("id", "id2");
    final String routing = "routing";

    final Script scriptWithParameters = mock(Script.class);
    when(scriptBuilder.getScriptWithParameters(script, params)).thenReturn(scriptWithParameters);

    // When
    batchRequest.upsertWithScriptAndRouting(INDEX, ID, entity, script, params, routing);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isUpdate()).isTrue();

    final var update = bulkOperation.update();
    assertThat(update.index()).isEqualTo(INDEX);
    assertThat(update.id()).isEqualTo(ID);
    assertThat(update.routing()).isEqualTo(routing);
  }

  @Test
  void shouldUpdateWithFields() throws PersistenceException, IOException {
    final Map<String, Object> updateFields = Map.of("id", "id2");

    // When
    batchRequest.update(INDEX, ID, updateFields);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isUpdate()).isTrue();

    final var update = bulkOperation.update();
    assertThat(update.index()).isEqualTo(INDEX);
    assertThat(update.id()).isEqualTo(ID);
  }

  @Test
  void shouldUpdateWithEntity() throws IOException, PersistenceException {
    // Given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);

    // When
    batchRequest.update(INDEX, ID, entity);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isUpdate()).isTrue();

    final var update = bulkOperation.update();
    assertThat(update.index()).isEqualTo(INDEX);
    assertThat(update.id()).isEqualTo(ID);
  }

  @Test
  void shouldUpdateWithScript() throws PersistenceException, IOException {
    // Given
    final String script = "script";
    final Map<String, Object> params = Map.of("id", "id2");

    final Script scriptWithParameters = mock(Script.class);
    when(scriptBuilder.getScriptWithParameters(script, params)).thenReturn(scriptWithParameters);

    // When
    batchRequest.updateWithScript(INDEX, ID, script, params);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isUpdate()).isTrue();

    final var update = bulkOperation.update();
    assertThat(update.index()).isEqualTo(INDEX);
    assertThat(update.id()).isEqualTo(ID);
  }

  @Test
  void shouldDeleteEntity() throws IOException, PersistenceException {
    // Given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);

    // When
    batchRequest.delete(INDEX, ID);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isDelete()).isTrue();

    final var delete = bulkOperation.delete();
    assertThat(delete.index()).isEqualTo(INDEX);
    assertThat(delete.id()).isEqualTo(ID);
  }

  @Test
  void shouldDeleteEntityWithRouting() throws IOException, PersistenceException {
    // given
    final String routing = "routing";

    // when
    batchRequest.deleteWithRouting(INDEX, ID, routing);
    batchRequest.execute(null);

    // then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that an index operation is added
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(1);

    final var bulkOperation = operations.getFirst();
    assertThat(bulkOperation.isDelete()).isTrue();

    final var delete = bulkOperation.delete();
    assertThat(delete.index()).isEqualTo(INDEX);
    assertThat(delete.id()).isEqualTo(ID);
    assertThat(delete.routing()).isEqualTo(routing);
  }

  @Test
  void shouldExecuteWithMultipleOperationsInBatch() throws PersistenceException, IOException {
    // Given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);
    // When
    batchRequest.add(INDEX, entity);
    batchRequest.update(INDEX, ID, entity);
    batchRequest.execute(null);

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());

    // verify that there are two operations in the bulk request
    final List<BulkOperation> operations = captor.getValue().operations();
    assertThat(operations).hasSize(2);
  }

  @Test
  void shouldExecuteWithRefresh() throws PersistenceException, IOException {
    // Given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);

    // When
    batchRequest.add(INDEX, entity);
    batchRequest.executeWithRefresh();

    // Then
    final ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(osClient).bulk(captor.capture());
    final BulkRequest request = captor.getValue();
    assertThat(request.refresh()).isEqualTo(Refresh.True);
  }

  @ParameterizedTest
  @ValueSource(classes = {IOException.class, OpenSearchException.class})
  void shouldThrowPersistenceExceptionIfBulkRequestFails(final Class<? extends Throwable> throwable)
      throws IOException {
    // Given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);

    // When
    batchRequest.add(INDEX, entity);
    when(osClient.bulk(any(BulkRequest.class))).thenThrow(throwable);

    // When
    final ThrowingCallable callable = () -> batchRequest.execute(null);

    // Then
    assertThatThrownBy(callable).isInstanceOf(PersistenceException.class);
    verify(osClient).bulk(any(BulkRequest.class));
  }

  @Test
  void shouldThrowPersistenceExceptionIfAResponseItemHasError() throws IOException {
    // Given
    final TestExporterEntity entity = new TestExporterEntity().setId(ID);

    final BulkResponseItem item = mock(BulkResponseItem.class);
    when(item.id()).thenReturn("1");
    when(item.error())
        .thenReturn(new ErrorCause.Builder().type("string_error").reason("error").build());

    final BulkResponse bulkResponse = mock(BulkResponse.class);
    when(bulkResponse.items()).thenReturn(List.of(item));

    when(osClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

    // When
    batchRequest.add(INDEX, entity);
    final ThrowingCallable callable = () -> batchRequest.execute(null);

    // Then
    assertThatThrownBy(callable).isInstanceOf(PersistenceException.class);
    verify(osClient).bulk(any(BulkRequest.class));
  }
}
