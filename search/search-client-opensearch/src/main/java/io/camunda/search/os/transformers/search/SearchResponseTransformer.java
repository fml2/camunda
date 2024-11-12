/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.os.transformers.search;

import io.camunda.search.clients.core.SearchQueryHit;
import io.camunda.search.clients.core.SearchQueryResponse;
import io.camunda.search.os.transformers.OpensearchTransformer;
import io.camunda.search.os.transformers.OpensearchTransformers;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHits;

public final class SearchResponseTransformer<T>
    extends OpensearchTransformer<SearchResponse<T>, SearchQueryResponse<T>> {

  public SearchResponseTransformer(final OpensearchTransformers transformers) {
    super(transformers);
  }

  @Override
  public SearchQueryResponse<T> apply(final SearchResponse<T> value) {
    final var hits = value.hits();
    final var scrollId = value.scrollId();

    final var total = hits.total();
    final var totalHits = of(total);

    final var sourceHits = hits.hits();
    final var transformedHits = of(sourceHits);

    return new SearchQueryResponse.Builder<T>()
        .totalHits(totalHits)
        .scrollId(scrollId)
        .hits(transformedHits)
        .build();
  }

  private List<SearchQueryHit<T>> of(final List<Hit<T>> hits) {
    if (hits != null) {
      final var hitTransformer = new SearchQueryHitTransformer<T>(transformers);
      return hits.stream().map(hitTransformer::apply).collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  private long of(final TotalHits totalHits) {
    if (totalHits != null) {
      return totalHits.value();
    }
    return 0;
  }
}
