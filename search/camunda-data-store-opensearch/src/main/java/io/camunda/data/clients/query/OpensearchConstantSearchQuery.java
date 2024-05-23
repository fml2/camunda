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
import org.opensearch.client.opensearch._types.query_dsl.ConstantScoreQuery;

public class OpensearchConstantSearchQuery extends OpensearchQueryVariant<ConstantScoreQuery>
    implements DataStoreConstantScoreQuery {

  public OpensearchConstantSearchQuery(final ConstantScoreQuery constantScoreQuery) {
    super(constantScoreQuery);
  }

  public static final class Builder implements DataStoreConstantScoreQuery.Builder {

    private ConstantScoreQuery.Builder wrappedBuilder;

    public Builder() {
      wrappedBuilder = new ConstantScoreQuery.Builder();
    }

    @Override
    public Builder filter(final DataStoreQuery query) {
      wrappedBuilder.filter(((OpensearchQuery) query).query());
      return null;
    }

    @Override
    public Builder filter(
        final Function<DataStoreQuery.Builder, DataStoreObjectBuilder<DataStoreQuery>> fn) {
      return filter(DataStoreQueryBuilders.query(fn));
    }

    @Override
    public DataStoreConstantScoreQuery build() {
      final var query = wrappedBuilder.build();
      return new OpensearchConstantSearchQuery(query);
    }
  }
}
