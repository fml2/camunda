/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.migrate313to86.indices.db;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.util.Locale;

public abstract class ProcessInstanceArchiveIndexV8<TBuilder>
    extends DefaultIndexMappingCreator<TBuilder> {
  public static final int VERSION = 8; // same as current processInstanceIndexVersion

  private final String indexName;

  public ProcessInstanceArchiveIndexV8(final String processInstanceIndexKey) {
    indexName = constructIndexName(processInstanceIndexKey);
  }

  public static String constructIndexName(final String processInstanceIndexKey) {
    return "process-instance-archive-" + processInstanceIndexKey.toLowerCase(Locale.ENGLISH);
  }

  @Override
  public String getIndexName() {
    return indexName;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public TypeMapping.Builder addProperties(final TypeMapping.Builder builder) {
    // adding just one field since this Index exists to test index deletion only
    return builder.properties("processDefinitionKey", p -> p.keyword(k -> k));
  }
}
