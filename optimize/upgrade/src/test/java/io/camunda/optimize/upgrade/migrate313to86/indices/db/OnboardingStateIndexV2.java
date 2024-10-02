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

public abstract class OnboardingStateIndexV2<TBuilder>
    extends DefaultIndexMappingCreator<TBuilder> {

  public static final int VERSION = 2;

  @Override
  public String getIndexName() {
    return "onboarding-state";
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public TypeMapping.Builder addProperties(final TypeMapping.Builder builder) {
    // @formatter:off
    return builder
        .properties("id", p -> p.keyword(k -> k))
        .properties("key", p -> p.keyword(k -> k))
        .properties("userId", p -> p.keyword(k -> k))
        .properties("seen", p -> p.boolean_(k -> k));
    // @formatter:on
  }
}
