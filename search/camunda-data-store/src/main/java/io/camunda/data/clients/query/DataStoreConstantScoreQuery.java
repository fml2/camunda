/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.data.clients.query;

import io.camunda.util.DataStoreObjectBuilder;
import java.util.function.Function;

public interface DataStoreConstantScoreQuery extends DataStoreQueryVariant {

  static DataStoreConstantScoreQuery of(
      final Function<Builder, DataStoreObjectBuilder<DataStoreConstantScoreQuery>> fn) {
    return DataStoreQueryBuilders.constantScore(fn);
  }

  public interface Builder extends DataStoreObjectBuilder<DataStoreConstantScoreQuery> {

    Builder filter(final DataStoreQuery query);

    Builder filter(
        final Function<DataStoreQuery.Builder, DataStoreObjectBuilder<DataStoreQuery>> fn);
  }
}
