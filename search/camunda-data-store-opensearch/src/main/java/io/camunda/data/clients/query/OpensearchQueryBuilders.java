/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.data.clients.query;

import io.camunda.data.clients.util.DataStoreQueryBuildersDelegate;
import io.camunda.util.DataStoreObjectBuilder;
import java.util.function.Function;

public class OpensearchQueryBuilders implements DataStoreQueryBuildersDelegate {

  public DataStoreBoolQuery.Builder bool() {
    return new OpensearchBoolQuery.Builder();
  }

  public DataStoreBoolQuery bool(
      final Function<DataStoreBoolQuery.Builder, DataStoreObjectBuilder<DataStoreBoolQuery>> fn) {
    return fn.apply(bool()).build();
  }

  public DataStoreConstantScoreQuery.Builder constantScore() {
    return new OpensearchConstantSearchQuery.Builder();
  }

  public DataStoreConstantScoreQuery constantScore(
      final Function<
              DataStoreConstantScoreQuery.Builder,
              DataStoreObjectBuilder<DataStoreConstantScoreQuery>>
          fn) {
    return fn.apply(constantScore()).build();
  }

  public DataStoreExistsQuery.Builder exists() {
    return new OpensearchExistsQuery.Builder();
  }

  public DataStoreExistsQuery exists(
      final Function<DataStoreExistsQuery.Builder, DataStoreObjectBuilder<DataStoreExistsQuery>>
          fn) {
    return fn.apply(exists()).build();
  }

  public DataStoreHasChildQuery.Builder hasChild() {
    return new OpensearchHasChildQuery.Builder();
  }

  public DataStoreHasChildQuery hasChild(
      final Function<DataStoreHasChildQuery.Builder, DataStoreObjectBuilder<DataStoreHasChildQuery>>
          fn) {
    return fn.apply(hasChild()).build();
  }

  public DataStoreIdsQuery.Builder ids() {
    return new OpensearchIdsQuery.Builder();
  }

  public DataStoreIdsQuery ids(
      final Function<DataStoreIdsQuery.Builder, DataStoreObjectBuilder<DataStoreIdsQuery>> fn) {
    return fn.apply(ids()).build();
  }

  public DataStoreMatchQuery.Builder match() {
    return new OpensearchMatchQuery.Builder();
  }

  public DataStoreMatchQuery match(
      final Function<DataStoreMatchQuery.Builder, DataStoreObjectBuilder<DataStoreMatchQuery>> fn) {
    return fn.apply(match()).build();
  }

  public DataStoreMatchAllQuery.Builder matchAll() {
    return new OpensearchMatchAllQuery.Builder();
  }

  public DataStoreMatchAllQuery matchAll(
      final Function<DataStoreMatchAllQuery.Builder, DataStoreObjectBuilder<DataStoreMatchAllQuery>>
          fn) {
    return fn.apply(matchAll()).build();
  }

  public DataStoreMatchNoneQuery.Builder matchNone() {
    return new OpensearchMatchNoneQuery.Builder();
  }

  public DataStoreMatchNoneQuery matchNone(
      final Function<
              DataStoreMatchNoneQuery.Builder, DataStoreObjectBuilder<DataStoreMatchNoneQuery>>
          fn) {
    return fn.apply(matchNone()).build();
  }

  public DataStorePrefixQuery.Builder prefix() {
    return new OpensearchPrefixQuery.Builder();
  }

  public DataStorePrefixQuery prefix(
      final Function<DataStorePrefixQuery.Builder, DataStoreObjectBuilder<DataStorePrefixQuery>>
          fn) {
    return fn.apply(prefix()).build();
  }

  public DataStoreQuery.Builder query() {
    return new OpensearchQuery.Builder();
  }

  public DataStoreQuery query(
      final Function<DataStoreQuery.Builder, DataStoreObjectBuilder<DataStoreQuery>> fn) {
    return fn.apply(query()).build();
  }

  public DataStoreRangeQuery.Builder range() {
    return new OpensearchRangeQuery.Builder();
  }

  public DataStoreRangeQuery range(
      final Function<DataStoreRangeQuery.Builder, DataStoreObjectBuilder<DataStoreRangeQuery>> fn) {
    return fn.apply(range()).build();
  }

  public DataStoreTermQuery.Builder term() {
    return new OpensearchTermQuery.Builder();
  }

  public DataStoreTermQuery term(
      final Function<DataStoreTermQuery.Builder, DataStoreObjectBuilder<DataStoreTermQuery>> fn) {
    return fn.apply(term()).build();
  }

  public DataStoreTermsQuery.Builder terms() {
    return new OpensearchTermsQuery.Builder();
  }

  public DataStoreTermsQuery terms(
      final Function<DataStoreTermsQuery.Builder, DataStoreObjectBuilder<DataStoreTermsQuery>> fn) {
    return fn.apply(terms()).build();
  }

  public DataStoreWildcardQuery.Builder wildcard() {
    return new OpensearchWildcardQuery.Builder();
  }

  public DataStoreWildcardQuery wildcard(
      final Function<DataStoreWildcardQuery.Builder, DataStoreObjectBuilder<DataStoreWildcardQuery>>
          fn) {
    return fn.apply(wildcard()).build();
  }
}
