/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.data.clients.query;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Operator;

public class OpensearchMatchQuery extends OpensearchQueryVariant<MatchQuery>
    implements DataStoreMatchQuery {

  public OpensearchMatchQuery(MatchQuery queryVariant) {
    super(queryVariant);
  }

  public static final class Builder implements DataStoreMatchQuery.Builder {

    private MatchQuery.Builder wrappedBuilder;

    public Builder() {
      this.wrappedBuilder = new MatchQuery.Builder();
    }

    @Override
    public Builder field(final String value) {
      wrappedBuilder.field(value);
      return this;
    }

    @Override
    public Builder query(final String query) {
      wrappedBuilder.query(FieldValue.of(query));
      return this;
    }

    @Override
    public Builder operator(String value) {
      wrappedBuilder.operator(Operator.valueOf(value));
      return this;
    }

    @Override
    public DataStoreMatchQuery build() {
      final var query = wrappedBuilder.build();
      return new OpensearchMatchQuery(query);
    }
  }
}
