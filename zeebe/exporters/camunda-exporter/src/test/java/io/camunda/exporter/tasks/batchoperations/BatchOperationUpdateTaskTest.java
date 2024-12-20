/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.tasks.batchoperations;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.exporter.tasks.batchoperations.BatchOperationUpdateRepository.DocumentUpdate;
import io.camunda.exporter.tasks.batchoperations.BatchOperationUpdateRepository.OperationsAggData;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchOperationUpdateTaskTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchOperationUpdateTaskTest.class);
  private final TestRepository repository = Mockito.spy(new TestRepository());
  private final BatchOperationUpdateTask task = new BatchOperationUpdateTask(repository, LOGGER);

  @Test
  void shouldReturnZeroIfNoBatchOperationsFound() {
    // given - when
    final var result = task.execute();

    // then
    assertThat(result).succeedsWithin(Duration.ZERO).isEqualTo(0);
  }

  @Test
  void shouldReturnZeroIfNoDocumentUpdatesRequired() {
    // given - when
    repository.batchOperationIds.add("1");
    final var result = task.execute();

    // then
    assertThat(result).succeedsWithin(Duration.ZERO).isEqualTo(0);
  }

  @Test
  void shouldUpdateBatchOperations() {
    // given - when
    repository.batchOperationIds.add("1");
    repository.batchOperationIds.add("2");
    repository.batchOperationIds.add("3");
    repository.finishedOperationsCount.add(new OperationsAggData("1", 5));
    repository.finishedOperationsCount.add(new OperationsAggData("2", 6));
    final var result = task.execute();

    // then
    assertThat(result).succeedsWithin(Duration.ZERO).isEqualTo(2);
    assertThat(repository.documentUpdates).hasSize(2);
    assertThat(repository.documentUpdates)
        .contains(new DocumentUpdate("1", 5L), new DocumentUpdate("2", 6L));
  }

  private static final class TestRepository implements BatchOperationUpdateRepository {
    List<String> batchOperationIds = new ArrayList<>();
    List<OperationsAggData> finishedOperationsCount = new ArrayList<>();
    private List<DocumentUpdate> documentUpdates = new ArrayList<>();

    @Override
    public List<String> getNotFinishedBatchOperations() {
      return batchOperationIds;
    }

    @Override
    public List<OperationsAggData> getFinishedOperationsCount(
        final List<String> batchOperationIds) {
      return finishedOperationsCount;
    }

    @Override
    public Integer bulkUpdate(final List<DocumentUpdate> documentUpdates) {
      this.documentUpdates = documentUpdates;
      return documentUpdates.size();
    }

    @Override
    public void close() throws Exception {}
  }
}
