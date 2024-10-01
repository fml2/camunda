/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.steps.document;

import io.camunda.optimize.service.db.DatabaseQueryWrapper;
import io.camunda.optimize.service.db.schema.IndexMappingCreator;
import io.camunda.optimize.upgrade.db.SchemaUpgradeClient;
import io.camunda.optimize.upgrade.steps.UpgradeStep;
import io.camunda.optimize.upgrade.steps.UpgradeStepType;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

@EqualsAndHashCode(callSuper = true)
public class UpdateDataStep extends UpgradeStep {

  private final DatabaseQueryWrapper queryWrapper;
  private final String updateScript;
  private Map<String, Object> parameters;
  private final Callable<Map<String, Object>> paramMapProvider;

  public UpdateDataStep(
      final IndexMappingCreator index,
      final DatabaseQueryWrapper queryWrapper,
      final String updateScript) {
    this(index, queryWrapper, updateScript, null, null);
  }

  public UpdateDataStep(
      final IndexMappingCreator index,
      final DatabaseQueryWrapper queryWrapper,
      final String updateScript,
      final Map<String, Object> parameters) {
    this(index, queryWrapper, updateScript, parameters, null);
  }

  public UpdateDataStep(
      final IndexMappingCreator index,
      final DatabaseQueryWrapper queryWrapper,
      final String updateScript,
      final Map<String, Object> parameters,
      final Callable<Map<String, Object>> paramMapProvider) {
    super(index);
    this.queryWrapper = queryWrapper;
    this.updateScript = updateScript;
    this.parameters = parameters;
    this.paramMapProvider = paramMapProvider;
  }

  @Override
  public UpgradeStepType getType() {
    return UpgradeStepType.DATA_UPDATE;
  }

  @Override
  @SneakyThrows
  public void performUpgradeStep(SchemaUpgradeClient<?, ?> schemaUpgradeClient) {
    if (paramMapProvider != null) {
      parameters = paramMapProvider.call();
    }
    schemaUpgradeClient.updateDataByIndexName(index, queryWrapper, updateScript, parameters);
  }
}
