/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service.search.query;

import io.camunda.service.search.filter.FilterBase;
import io.camunda.service.search.result.QueryResultConfig;
import io.camunda.service.search.sort.SortOption;

public interface TypedSearchQuery<F extends FilterBase, S extends SortOption>
    extends SearchQueryBase {

  F filter();

  S sort();

  default QueryResultConfig resultConfig() {
    return null;
  }
}
