/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.api;

import io.camunda.zeebe.logstreams.log.LogStreamBatchWriter;
import io.camunda.zeebe.streamprocessor.records.ImmutableRecordBatch;

/**
 * Here the interface is just a suggestion. Can be whatever PDT teams thinks is best to work with
 */
public interface ProcessingResult {

  @Deprecated
  long writeRecordsToStream(LogStreamBatchWriter logStreamBatchWriter);

  /**
   * Returns the resulting record batch, which can be empty or consist of multiple {@link
   * io.camunda.zeebe.streamprocessor.records.RecordBatchEntry}s. These entries are the result of
   * the current processing. If an entry is of type {@link
   * io.camunda.zeebe.protocol.record.RecordType#COMMAND} it will be later processed as follow-up
   * command.
   *
   * @return returns the resulting immutable record batch
   */
  public ImmutableRecordBatch getRecordBatch();

  boolean writeResponse(CommandResponseWriter commandResponseWriter);

  /**
   * @return <code>false</code> to indicate that the side effect could not be applied successfully
   */
  boolean executePostCommitTasks();
}
