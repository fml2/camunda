/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.data.clients.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import io.camunda.util.DataStoreObjectBuilder;
import java.util.function.Function;

public final class ElasticsearchQuery implements DataStoreQuery {

  private final Query wrappedQuery;

  public ElasticsearchQuery(final Query query) {
    wrappedQuery = query;
  }

  public <T extends QueryVariant> ElasticsearchQuery(final ElasticsearchQueryVariant<T> query) {
    wrappedQuery = new Query(query.queryVariant());
  }

  public Query query() {
    return wrappedQuery;
  }

  public static final class Builder implements DataStoreQuery.Builder {

    private final Query.Builder wrappedBuilder;

    public Builder() {
      this.wrappedBuilder = new Query.Builder();
    }

    @Override
    public Builder bool(final DataStoreBoolQuery query) {
      wrappedBuilder.bool(((ElasticsearchBoolQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder bool(
        final Function<DataStoreBoolQuery.Builder, DataStoreObjectBuilder<DataStoreBoolQuery>> fn) {
      return bool(DataStoreQueryBuilders.bool(fn));
    }

    @Override
    public Builder constantScore(final DataStoreConstantScoreQuery query) {
      wrappedBuilder.constantScore(((ElasticsearchConstantSearchQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder constantScore(
        final Function<
                DataStoreConstantScoreQuery.Builder,
                DataStoreObjectBuilder<DataStoreConstantScoreQuery>>
            fn) {
      return constantScore(DataStoreQueryBuilders.constantScore(fn));
    }

    @Override
    public Builder exists(final DataStoreExistsQuery query) {
      wrappedBuilder.exists(((ElasticsearchExistsQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder exists(
        final Function<DataStoreExistsQuery.Builder, DataStoreObjectBuilder<DataStoreExistsQuery>>
            fn) {
      return exists(DataStoreQueryBuilders.exists(fn));
    }

    @Override
    public Builder hasChild(final DataStoreHasChildQuery query) {
      wrappedBuilder.hasChild(((ElasticsearchHasChildQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder hasChild(
        final Function<
                DataStoreHasChildQuery.Builder, DataStoreObjectBuilder<DataStoreHasChildQuery>>
            fn) {
      return hasChild(DataStoreQueryBuilders.hasChild(fn));
    }

    @Override
    public Builder ids(final DataStoreIdsQuery query) {
      wrappedBuilder.ids(((ElasticsearchIdsQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder ids(
        final Function<DataStoreIdsQuery.Builder, DataStoreObjectBuilder<DataStoreIdsQuery>> fn) {
      return ids(DataStoreQueryBuilders.ids(fn));
    }

    @Override
    public Builder match(final DataStoreMatchQuery query) {
      wrappedBuilder.match(((ElasticsearchMatchQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder match(
        final Function<DataStoreMatchQuery.Builder, DataStoreObjectBuilder<DataStoreMatchQuery>>
            fn) {
      return match(DataStoreQueryBuilders.match(fn));
    }

    @Override
    public Builder matchAll(final DataStoreMatchAllQuery query) {
      wrappedBuilder.matchAll(((ElasticsearchMatchAllQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder matchAll(
        final Function<
                DataStoreMatchAllQuery.Builder, DataStoreObjectBuilder<DataStoreMatchAllQuery>>
            fn) {
      return matchAll(DataStoreQueryBuilders.matchAll(fn));
    }

    @Override
    public Builder matchNone(final DataStoreMatchNoneQuery query) {
      wrappedBuilder.matchNone(((ElasticsearchMatchNoneQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder matchNone(
        final Function<
                DataStoreMatchNoneQuery.Builder, DataStoreObjectBuilder<DataStoreMatchNoneQuery>>
            fn) {
      return matchNone(DataStoreQueryBuilders.matchNone(fn));
    }

    @Override
    public Builder prefix(final DataStorePrefixQuery query) {
      wrappedBuilder.prefix(((ElasticsearchPrefixQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder prefix(
        final Function<DataStorePrefixQuery.Builder, DataStoreObjectBuilder<DataStorePrefixQuery>>
            fn) {
      return prefix(DataStoreQueryBuilders.prefix(fn));
    }

    @Override
    public Builder range(final DataStoreRangeQuery query) {
      wrappedBuilder.range(((ElasticsearchRangeQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder range(
        final Function<DataStoreRangeQuery.Builder, DataStoreObjectBuilder<DataStoreRangeQuery>>
            fn) {
      return range(DataStoreQueryBuilders.range(fn));
    }

    @Override
    public Builder term(final DataStoreTermQuery query) {
      wrappedBuilder.term(((ElasticsearchTermQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder term(
        final Function<DataStoreTermQuery.Builder, DataStoreObjectBuilder<DataStoreTermQuery>> fn) {
      return term(DataStoreQueryBuilders.term(fn));
    }

    @Override
    public Builder terms(final DataStoreTermsQuery query) {
      wrappedBuilder.terms(((ElasticsearchTermsQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder terms(
        final Function<DataStoreTermsQuery.Builder, DataStoreObjectBuilder<DataStoreTermsQuery>>
            fn) {
      return terms(DataStoreQueryBuilders.terms(fn));
    }

    @Override
    public Builder wildcard(final DataStoreWildcardQuery query) {
      wrappedBuilder.wildcard(((ElasticsearchWildcardQuery) query).queryVariant());
      return this;
    }

    @Override
    public Builder wildcard(
        final Function<
                DataStoreWildcardQuery.Builder, DataStoreObjectBuilder<DataStoreWildcardQuery>>
            fn) {
      return wildcard(DataStoreQueryBuilders.wildcard(fn));
    }

    @Override
    public DataStoreQuery build() {
      final var query = wrappedBuilder.build();
      return new ElasticsearchQuery(query);
    }
  }
}
